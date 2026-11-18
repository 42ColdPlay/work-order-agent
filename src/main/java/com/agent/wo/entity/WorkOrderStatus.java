package com.agent.wo.entity;

/**
 * 工单状态枚举。
 */
public enum WorkOrderStatus {

    PENDING("待处理"),
    IN_PROGRESS("处理中"),
    PENDING_APPROVAL("待审批"),
    APPROVED("已通过"),
    REJECTED("已驳回"),
    COMPLETED("已完成"),
    CLOSED("已关闭");

    private final String displayName;

    WorkOrderStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
