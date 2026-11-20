package com.agent.memory.chat;

import com.agent.memory.MemoryProperties;
import com.agent.memory.entity.ChatMessageEntity;
import com.agent.memory.repository.ChatMessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 会话记忆：Redis 做热缓存，MySQL 做持久化流水。
 * <p>
 * 读写策略：
 * - 写：先落 MySQL（可信来源），再写 Redis（失败只记日志，不影响本轮对话）
 * - 读：优先读 Redis；缓存未命中（TTL 过期 / 重启）时从 MySQL 重建并回填
 * - 裁剪：Redis 按条数裁剪，注入模型前再按 token 预算裁剪
 * <p>
 * 这样 Redis 挂掉只会让首轮变慢，不会丢历史；换存储介质也不用改窗口逻辑。
 */
@Component
public class HybridChatMemory implements ChatMemory {

    private static final Logger log = LoggerFactory.getLogger(HybridChatMemory.class);

    private static final String KEY_PREFIX = "chat:memory:";

    private final StringRedisTemplate redisTemplate;
    private final ChatMessageCodec codec;
    private final ChatMessageRepository repository;
    private final MemoryProperties properties;

    public HybridChatMemory(StringRedisTemplate redisTemplate,
                            ChatMessageCodec codec,
                            ChatMessageRepository repository,
                            MemoryProperties properties) {
        this.redisTemplate = redisTemplate;
        this.codec = codec;
        this.repository = repository;
        this.properties = properties;
    }

    @Override
    public void add(String conversationId, Message message) {
        add(conversationId, List.of(message));
    }

    @Override
    public void add(String conversationId, List<Message> messages) {
        if (messages == null || messages.isEmpty()) {
            return;
        }

        List<String> payloads = new ArrayList<>(messages.size());
        List<ChatMessageEntity> rows = new ArrayList<>(messages.size());
        for (Message message : messages) {
            try {
                String payload = codec.serialize(message);
                payloads.add(payload);
                rows.add(new ChatMessageEntity(conversationId, message.getMessageType().name(), payload));
            } catch (Exception e) {
                log.warn("消息序列化失败，跳过一条（type={}）: {}", message.getMessageType(), e.getMessage());
            }
        }
        if (payloads.isEmpty()) {
            return;
        }

        persist(rows);
        cache(conversationId, payloads);
    }

    @Override
    public List<Message> get(String conversationId, int lastN) {
        int window = Math.max(1, Math.min(lastN, properties.getChat().getMaxMessages()));

        List<Message> messages = readCache(conversationId, window);
        if (messages.isEmpty()) {
            messages = readDatabase(conversationId, window);
            if (!messages.isEmpty()) {
                refillCache(conversationId, messages);
            }
        }

        return MessageWindow.apply(messages, properties.getChat().getTokenBudget());
    }

    @Override
    @Transactional
    public void clear(String conversationId) {
        try {
            redisTemplate.delete(key(conversationId));
        } catch (Exception e) {
            log.warn("清除 Redis 会话记忆失败，继续清理数据库: {}", e.getMessage());
        }
        repository.deleteByConversationId(conversationId);
    }

    private void persist(List<ChatMessageEntity> rows) {
        if (!properties.getChat().isPersist()) {
            return;
        }
        try {
            repository.saveAll(rows);
        } catch (Exception e) {
            // 落库失败时降级为纯 Redis 缓存，至少保证当前会话还能继续
            log.error("对话流水落库失败，降级为仅 Redis 缓存: {}", e.getMessage());
        }
    }

    private void cache(String conversationId, List<String> payloads) {
        try {
            String key = key(conversationId);
            redisTemplate.opsForList().rightPushAll(key, payloads);
            redisTemplate.opsForList().trim(key, -properties.getChat().getMaxMessages(), -1);
            redisTemplate.expire(key, properties.getChat().getTtl());
        } catch (Exception e) {
            log.warn("Redis 会话记忆写入失败（历史仍在 MySQL）: {}", e.getMessage());
        }
    }

    private List<Message> readCache(String conversationId, int window) {
        try {
            List<String> jsonList = redisTemplate.opsForList().range(key(conversationId), -window, -1);
            return decode(jsonList);
        } catch (Exception e) {
            log.warn("读取 Redis 会话记忆失败，回源数据库: {}", e.getMessage());
            return List.of();
        }
    }

    private List<Message> readDatabase(String conversationId, int window) {
        if (!properties.getChat().isPersist()) {
            return List.of();
        }
        try {
            List<ChatMessageEntity> rows = repository.findByConversationIdOrderByIdDesc(
                    conversationId, PageRequest.of(0, window));
            Collections.reverse(rows);
            List<String> payloads = rows.stream().map(ChatMessageEntity::getPayload).toList();
            return decode(payloads);
        } catch (Exception e) {
            log.warn("从数据库重建会话记忆失败: {}", e.getMessage());
            return List.of();
        }
    }

    private List<Message> decode(List<String> jsonList) {
        if (jsonList == null || jsonList.isEmpty()) {
            return List.of();
        }
        List<Message> messages = new ArrayList<>(jsonList.size());
        for (String json : jsonList) {
            try {
                messages.add(codec.deserialize(json));
            } catch (Exception e) {
                // 单条历史损坏时跳过，避免整个会话的记忆不可用
                log.warn("跳过无法反序列化的历史消息: {}", e.getMessage());
            }
        }
        return messages;
    }

    private void refillCache(String conversationId, List<Message> messages) {
        try {
            String key = key(conversationId);
            Long size = redisTemplate.opsForList().size(key);
            if (size != null && size > 0) {
                return;
            }
            List<String> payloads = new ArrayList<>(messages.size());
            for (Message message : messages) {
                payloads.add(codec.serialize(message));
            }
            redisTemplate.opsForList().rightPushAll(key, payloads);
            redisTemplate.opsForList().trim(key, -properties.getChat().getMaxMessages(), -1);
            redisTemplate.expire(key, properties.getChat().getTtl());
            log.debug("已从数据库回填会话记忆缓存: conversationId={}, {} 条", conversationId, payloads.size());
        } catch (Exception e) {
            log.warn("回填会话记忆缓存失败: {}", e.getMessage());
        }
    }

    private String key(String conversationId) {
        return KEY_PREFIX + conversationId;
    }
}
