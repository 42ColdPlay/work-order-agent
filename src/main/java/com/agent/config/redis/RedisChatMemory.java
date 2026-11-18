package com.agent.config.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class RedisChatMemory implements ChatMemory {

    private static final String KEY_PREFIX = "chat:memory:";
    private static final int MAX_MESSAGES_PER_CONVERSATION = 100;

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisChatMemory(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void add(String conversationId, Message message) {
        String key = key(conversationId);
        try {
            String json = serialize(message);
            redisTemplate.opsForList().rightPush(key, json);
            trimList(key);
        } catch (Exception e) {
            throw new RuntimeException("序列化消息失败", e);
        }
    }

    @Override
    public void add(String conversationId, List<Message> messages) {
        String key = key(conversationId);
        try {
            for (Message message : messages) {
                String json = serialize(message);
                redisTemplate.opsForList().rightPush(key, json);
            }
            trimList(key);
        } catch (Exception e) {
            throw new RuntimeException("序列化消息失败", e);
        }
    }

    @Override
    public List<Message> get(String conversationId, int lastN) {
        String key = key(conversationId);
        List<String> jsonList = redisTemplate.opsForList().range(key, -lastN, -1);
        if (jsonList == null || jsonList.isEmpty()) return List.of();

        List<Message> messages = new ArrayList<>();
        for (String json : jsonList) {
            try {
                messages.add(deserialize(json));
            } catch (Exception e) {
                throw new RuntimeException("反序列化消息失败", e);
            }
        }
        return messages;
    }

    @Override
    public void clear(String conversationId) {
        redisTemplate.delete(key(conversationId));
    }

    private String key(String conversationId) {
        return KEY_PREFIX + conversationId;
    }

    private void trimList(String key) {
        redisTemplate.opsForList().trim(key, -MAX_MESSAGES_PER_CONVERSATION, -1);
    }

    private String serialize(Message message) throws JsonProcessingException {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("type", message.getMessageType().name());
        map.put("content", message.getText());
        if (message.getMetadata() != null && !message.getMetadata().isEmpty()) {
            map.put("metadata", message.getMetadata());
        }
        return objectMapper.writeValueAsString(map);
    }

    private Message deserialize(String json) throws JsonProcessingException {
        @SuppressWarnings("unchecked")
        Map<String, Object> map = objectMapper.readValue(json, Map.class);
        String type = (String) map.get("type");
        String content = (String) map.get("content");

        @SuppressWarnings("unchecked")
        Map<String, Object> metadata = (Map<String, Object>) map.get("metadata");
        if (metadata == null) metadata = Map.of();

        MessageType messageType = MessageType.valueOf(type);
        return switch (messageType) {
            case USER -> new UserMessage(content, java.util.Collections.emptyList(), metadata);
            case ASSISTANT -> new AssistantMessage(content, metadata);
            case SYSTEM -> new org.springframework.ai.chat.messages.SystemMessage(content);
            case TOOL -> throw new IllegalArgumentException("暂不支持反序列化 Tool 消息");
        };
    }
}