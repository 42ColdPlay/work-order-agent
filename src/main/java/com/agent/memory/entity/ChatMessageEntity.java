package com.agent.memory.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * 对话消息流水（记忆的唯一可信来源）。
 * <p>
 * 存的是编码后的 JSON，与 Redis 缓存中的结构完全一致，
 * 因此缓存过期后可以直接从这张表重建，不需要单独的转换逻辑。
 */
@Entity
@Table(name = "chat_message", indexes = {
        @Index(name = "idx_chat_message_conversation", columnList = "conversation_id")
})
public class ChatMessageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "conversation_id", nullable = false, length = 64)
    private String conversationId;

    @Column(name = "message_type", nullable = false, length = 16)
    private String messageType;

    // 必须显式 LONGTEXT：Hibernate 6 会把 @Lob String 映射成 tinytext(255)，装不下带 toolCalls 的消息
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String payload;

    @Column(name = "created_at", nullable = false, updatable = false)
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
