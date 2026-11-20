package com.agent.memory.entity;

import java.time.LocalDateTime;

/**
 * 对话消息流水（记忆的唯一可信来源）。
 * <p>
 * 存的是编码后的 JSON，与 Redis 缓存中的结构完全一致，
 * 因此缓存过期后可以直接从这张表重建，不需要单独的转换逻辑。
 */
public class ChatMessageEntity {

    private Long id;

    private String conversationId;

    private String messageType;

    // 对应 LONGTEXT：装得下带 toolCalls 的完整消息 JSON
    private String payload;

    private LocalDateTime createdAt = LocalDateTime.now();

    public ChatMessageEntity() {
    }

    public ChatMessageEntity(String conversationId, String messageType, String payload) {
        this.conversationId = conversationId;
        this.messageType = messageType;
        this.payload = payload;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
