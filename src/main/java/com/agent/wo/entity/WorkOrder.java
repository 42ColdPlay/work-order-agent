package com.agent.wo.entity;

import java.time.LocalDateTime;

/**
 * 工单实体。
 */
public class WorkOrder {

    private String id;
    private String title;
    private String description;
    private String priority;        // HIGH / MEDIUM / LOW
    private WorkOrderStatus status;
    private String reporterId;      // 创建人
    private String assigneeId;      // 当前处理人
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public WorkOrder() {}

    public WorkOrder(String id, String title, String description, String priority,
                     WorkOrderStatus status, String reporterId, String assigneeId) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.priority = priority;
        this.status = status;
        this.reporterId = reporterId;
        this.assigneeId = assigneeId;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // getter / setter
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public WorkOrderStatus getStatus() { return status; }
    public void setStatus(WorkOrderStatus status) { this.status = status; }

    public String getReporterId() { return reporterId; }
    public void setReporterId(String reporterId) { this.reporterId = reporterId; }

    public String getAssigneeId() { return assigneeId; }
    public void setAssigneeId(String assigneeId) { this.assigneeId = assigneeId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
