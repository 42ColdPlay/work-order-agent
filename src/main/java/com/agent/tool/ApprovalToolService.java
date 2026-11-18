package com.agent.tool;

import com.agent.wo.service.WorkOrderMockService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

/**
 * 审批流转相关工具。
 */
@Service
public class ApprovalToolService {

    private final WorkOrderMockService workOrderService;

    public ApprovalToolService(WorkOrderMockService workOrderService) {
        this.workOrderService = workOrderService;
    }

    @Tool(description = "审批通过工单，将工单流转到下一个节点")
    public String approveWorkOrder(
            @ToolParam(description = "工单 ID") String workOrderId,
            @ToolParam(description = "审批意见") String comment) {
        workOrderService.approve(workOrderId, comment);
        return "工单 " + workOrderId + " 已审批通过。";
    }

    @Tool(description = "驳回工单，退回到上一节点")
    public String rejectWorkOrder(
            @ToolParam(description = "工单 ID") String workOrderId,
            @ToolParam(description = "驳回原因") String reason) {
        workOrderService.reject(workOrderId, reason);
        return "工单 " + workOrderId + " 已驳回，原因：" + reason;
    }

    @Tool(description = "查询指定用户的待审批工单列表")
    public String getPendingApprovals(
            @ToolParam(description = "审批人用户 ID") String approverId) {
        var list = workOrderService.getPendingApprovals(approverId);
        if (list.isEmpty()) {
            return "暂无待审批工单。";
        }
        StringBuilder sb = new StringBuilder("待审批工单列表：\n");
        for (int i = 0; i < list.size(); i++) {
            var wo = list.get(i);
            sb.append(i + 1).append(". [").append(wo.getId()).append("] ")
                    .append(wo.getTitle()).append(" — ").append(wo.getStatus().getDisplayName()).append("\n");
        }
        return sb.toString();
    }
}
