package com.agent.memory.longterm;

import com.agent.memory.MemoryProperties;
import com.agent.memory.entity.MemoryStatus;
import com.agent.memory.entity.UserMemoryEntity;
import com.agent.memory.mapper.UserMemoryMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 用户长期记忆：写入、召回、清理。
 * <p>
 * 存储组合：
 * - 向量库：语义召回（metadata 中的 namespace 按用户隔离）
 * - MySQL：结构化元数据、状态流转、审计
 * <p>
 * 冲突策略（轻量版）：
 * - 相似度 >= duplicateThreshold：视为重复，跳过
 * - 相似度在 [conflictThreshold, duplicateThreshold)：视为"同一事实的新版本"，
 *   旧记忆在向量库删除、在 MySQL 标记 SUPERSEDED，再写入新记忆
 * - 其余：直接新增
 */
@Service
public class LongTermMemoryService {

    private static final Logger log = LoggerFactory.getLogger(LongTermMemoryService.class);

    private static final String NAMESPACE_PREFIX = "user-memory:";
    private static final double CONFLICT_SEARCH_THRESHOLD = 0.5;

    private final VectorStore vectorStore;
    private final UserMemoryMapper memoryMapper;
    private final MemoryProperties properties;

    public LongTermMemoryService(VectorStore vectorStore,
                                 UserMemoryMapper memoryMapper,
                                 MemoryProperties properties) {
        this.vectorStore = vectorStore;
        this.memoryMapper = memoryMapper;
        this.properties = properties;
    }

    /**
     * 按当前问题召回该用户的长期记忆。向量库不可用时返回空列表，不影响主流程。
     */
    public List<MemoryItem> recall(String userId, String query) {
        if (!properties.getLongterm().isEnabled() || isBlank(userId) || isBlank(query)) {
            return List.of();
        }
        try {
            SearchRequest request = SearchRequest.builder()
                    .query(query)
                    .topK(properties.getLongterm().getTopK())
                    .similarityThreshold(properties.getLongterm().getRecallThreshold())
                    .filterExpression(namespaceFilter(userId))
                    .build();

            List<Document> documents = vectorStore.similaritySearch(request);
            List<MemoryItem> items = new ArrayList<>(documents.size());
            for (Document document : documents) {
                items.add(new MemoryItem(
                        document.getText(),
                        str(document.getMetadata().get("category")),
                        intOf(document.getMetadata().get("importance")),
                        str(document.getMetadata().get("createdAt")),
                        document.getScore() == null ? 0 : document.getScore()));
            }
            if (!items.isEmpty()) {
                log.debug("召回用户 [{}] 长期记忆 {} 条", userId, items.size());
            }
            return items;
        } catch (Exception e) {
            // 向量库不可用（Chroma 未启动、Embedding 服务异常）时静默降级
            log.warn("长期记忆召回失败，本轮不注入记忆: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * 把召回结果格式化为可拼进 system prompt 的文本块；没有记忆时返回空串。
     */
    public String formatForPrompt(List<MemoryItem> items) {
        if (items == null || items.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder("以下是过往会话中沉淀的关于当前用户的长期记忆：\n=====\n");
        for (MemoryItem item : items) {
            sb.append("- ");
            String createdAt = item.createdAt();
            if (createdAt != null && createdAt.length() >= 10) {
                sb.append("[").append(createdAt, 0, 10).append("] ");
            }
            sb.append(item.content()).append("\n");
        }
        sb.append("=====\n")
                .append("这些信息可能有助于理解用户背景；若与当前问题无关，直接忽略，不要主动复述。");
        return sb.toString();
    }

    /**
     * 写入一条记忆（自带去重与覆盖判断）。
     */
    public void save(String userId, String content, String category, int importance, String conversationId) {
        if (!properties.getLongterm().isEnabled() || isBlank(userId) || isBlank(content)) {
            return;
        }
        String fact = content.trim();
        if (fact.length() < 4) {
            return;
        }
        if (fact.length() > 300) {
            fact = fact.substring(0, 300);
        }

        String supersededVectorId = null;
        List<Document> similar = searchSimilar(userId, fact);
        if (!similar.isEmpty()) {
            Document top = similar.get(0);
            double score = top.getScore() == null ? 0 : top.getScore();
            if (score >= properties.getLongterm().getDuplicateThreshold()) {
                log.debug("记忆已存在，跳过: {}", fact);
                return;
            }
            if (score >= properties.getLongterm().getConflictThreshold()) {
                supersededVectorId = top.getId();
            }
        }

        if (supersededVectorId != null) {
            supersede(supersededVectorId);
        }

        int importanceScore = Math.max(1, Math.min(10, importance));
        String vectorId = UUID.randomUUID().toString();
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("namespace", namespace(userId));
        metadata.put("userId", userId);
        metadata.put("category", isBlank(category) ? "other" : category);
        metadata.put("importance", importanceScore);
        metadata.put("createdAt", LocalDateTime.now().toString());
        metadata.put("sourceConversationId", conversationId == null ? "" : conversationId);

        try {
            vectorStore.add(List.of(new Document(vectorId, fact, metadata)));
        } catch (Exception e) {
            // 向量写入失败就不落 MySQL，避免出现"库里有、检索不到"的脏数据
            log.error("长期记忆写入向量库失败，本条丢弃: {}", e.getMessage());
            return;
        }

        UserMemoryEntity entity = new UserMemoryEntity();
        entity.setUserId(userId);
        entity.setContent(fact);
        entity.setCategory(isBlank(category) ? "other" : category);
        entity.setImportance(importanceScore);
        entity.setVectorId(vectorId);
        entity.setSourceConversationId(conversationId);
        entity.setStatus(MemoryStatus.ACTIVE);
        try {
            memoryMapper.insert(entity);
        } catch (Exception e) {
            log.error("长期记忆落库失败（向量库已写入，可能出现不可见记忆）: {}", e.getMessage());
        }
    }

    /** 列表查询：只返回生效中的记忆 */
    public List<MemoryItem> list(String userId) {
        if (isBlank(userId)) {
            return List.of();
        }
        return memoryMapper.findActiveByUserId(userId, MemoryStatus.ACTIVE, 50)
                .stream()
                .map(entity -> new MemoryItem(
                        entity.getContent(),
                        entity.getCategory(),
                        entity.getImportance() == null ? 5 : entity.getImportance(),
                        entity.getCreatedAt() == null ? null : entity.getCreatedAt().toString(),
                        0))
                .toList();
    }

    /** 清除该用户的全部长期记忆（合规删除入口） */
    @Transactional
    public void clear(String userId) {
        if (isBlank(userId)) {
            return;
        }
        try {
            vectorStore.delete(namespaceFilter(userId));
        } catch (Exception e) {
            log.warn("清除向量库记忆失败，继续清理数据库: {}", e.getMessage());
        }
        long deleted = memoryMapper.deleteByUserId(userId);
        log.info("已清除用户 [{}] 的长期记忆 {} 条", userId, deleted);
    }

    private List<Document> searchSimilar(String userId, String fact) {
        try {
            SearchRequest request = SearchRequest.builder()
                    .query(fact)
                    .topK(1)
                    .similarityThreshold(CONFLICT_SEARCH_THRESHOLD)
                    .filterExpression(namespaceFilter(userId))
                    .build();
            return vectorStore.similaritySearch(request);
        } catch (Exception e) {
            log.warn("记忆去重检索失败，按新记忆处理: {}", e.getMessage());
            return List.of();
        }
    }

    private void supersede(String vectorId) {
        try {
            vectorStore.delete(List.of(vectorId));
        } catch (Exception e) {
            log.warn("删除旧记忆向量失败: {}", e.getMessage());
        }
        memoryMapper.findByVectorId(vectorId).ifPresent(old -> {
            memoryMapper.updateStatus(old.getId(), MemoryStatus.SUPERSEDED);
            log.info("记忆已被新事实取代: {}", old.getContent());
        });
    }

    private String namespace(String userId) {
        return NAMESPACE_PREFIX + userId;
    }

    private Filter.Expression namespaceFilter(String userId) {
        return new FilterExpressionBuilder().eq("namespace", namespace(userId)).build();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String str(Object value) {
        return value == null ? "" : value.toString();
    }

    private int intOf(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return value == null ? 5 : Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return 5;
        }
    }
}
