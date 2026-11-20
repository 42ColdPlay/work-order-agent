package com.agent.memory.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * 用户长期记忆（抽取出来的事实）。
 * <p>
 * MySQL 保存结构化元数据用于列表、审计和状态流转；
 * 向量库里保存同一份文本（id = vectorId），用于语义召回。
 */
@Entity
@Table(name = "user_memory", indexes = {
        @Index(name = "idx_user_memory_user", columnList = "user_id")
})
public class UserMemoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;

    // 显式 LONGTEXT，避免 Hibernate 6 把 @Lob String 映射成 tinytext(255)
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String content;

    @Column(length = 32)
    private String category;

    @Column(nullable = false)
    private Integer importance = 5;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private MemoryStatus status = MemoryStatus.ACTIVE;

    /** 对应向量库中的文档 ID */
    @Column(name = "vector_id", length = 64)
    private String vectorId;

    @Column(name = "source_conversation_id", length = 64)
    private String sourceConversationId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    public UserMemoryEntity() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Integer getImportance() {
        return importance;
    }

    public void setImportance(Integer importance) {
        this.importance = importance;
    }

    public MemoryStatus getStatus() {
        return status;
    }

    public void setStatus(MemoryStatus status) {
        this.status = status;
    }

    public String getVectorId() {
        return vectorId;
    }

    public void setVectorId(String vectorId) {
        this.vectorId = vectorId;
    }

    public String getSourceConversationId() {
        return sourceConversationId;
    }

    public void setSourceConversationId(String sourceConversationId) {
        this.sourceConversationId = sourceConversationId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
