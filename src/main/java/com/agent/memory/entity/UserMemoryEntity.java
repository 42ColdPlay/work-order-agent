package com.agent.memory.entity;

import java.time.LocalDateTime;

/**
 * 用户长期记忆（抽取出来的事实）。
 * <p>
 * MySQL 保存结构化元数据用于列表、审计和状态流转；
 * 向量库里保存同一份文本（id = vectorId），用于语义召回。
 */
public class UserMemoryEntity {

    private Long id;

    private String userId;

    // 对应 LONGTEXT
    private String content;

    private String category;

    private Integer importance = 5;

    private MemoryStatus status = MemoryStatus.ACTIVE;

    /** 对应向量库中的文档 ID */
    private String vectorId;

    private String sourceConversationId;

    private LocalDateTime createdAt = LocalDateTime.now();

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
