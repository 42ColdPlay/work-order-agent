package com.agent.tool;

import com.agent.wo.entity.WorkOrder;
import com.agent.wo.entity.WorkOrderStatus;
import com.agent.wo.service.WorkOrderMockService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 工单查询与创建相关工具。
 * <p>
 * 每个 @Tool 方法会自动生成 OpenAI Function Calling Schema，
 * 大模型在推理时根据用户意图自动选择和调用。
 * <p>
 * 方法上的注释会被大模型读取作为工具描述，务必写清楚。
 */
@Service
public class WorkOrderToolService {

    private final WorkOrderMockService workOrderService;

    public WorkOrderToolService(WorkOrderMockService workOrderService) {
        this.workOrderService = workOrderService;
    }

    @Tool(description = "根据工单 ID 查询工单的详细信息，包括标题、状态、当前处理人、创建时间等")
    public WorkOrder getWorkOrderById(
            @ToolParam(description = "工单 ID") String workOrderId) {
        return workOrderService.getById(workOrderId);
    }

    @Tool(description = "查询指定处理人的所有待办工单列表")
    public List<WorkOrder> getPendingWorkOrders(
            @ToolParam(description = "处理人用户 ID") String assigneeId) {
        return workOrderService.getPendingOrders(assigneeId);
    }

    @Tool(description = "创建一条新的工单，返回创建后的工单 ID")
    public String createWorkOrder(
            @ToolParam(description = "工单标题") String title,
            @ToolParam(description = "工单描述/内容") String description,
            @ToolParam(description = "优先级: HIGH/MEDIUM/LOW") String priority,
            @ToolParam(description = "创建人用户 ID") String reporterId,
            @ToolParam(description = "处理人用户 ID") String assigneeId) {
        return workOrderService.create(title, description, priority, reporterId, assigneeId);
    }

    @Tool(description = "将工单转派给其他处理人")
    public boolean reassignWorkOrder(
            @ToolParam(description = "工单 ID") String workOrderId,
            @ToolParam(description = "新的处理人用户 ID") String newAssigneeId) {
        return workOrderService.reassign(workOrderId, newAssigneeId);
    }

    @Tool(description = "查询工单处理进度和当前所在的审批节点")
    public String getWorkOrderProgress(
            @ToolParam(description = "工单 ID") String workOrderId) {
        return workOrderService.getProgress(workOrderId);
    }

    @Tool(description = "按关键字搜索工单，支持按标题/描述模糊匹配，同时可按状态、优先级、处理人等条件过滤")
    public List<WorkOrder> searchWorkOrders(
            @ToolParam(description = "搜索关键词，匹配标题和描述") String keyword,
            @ToolParam(description = "按状态过滤：PENDING / IN_PROGRESS / PENDING_APPROVAL / APPROVED / REJECTED / COMPLETED / CLOSED，可选") String status,
            @ToolParam(description = "按优先级过滤：HIGH / MEDIUM / LOW，可选") String priority,
            @ToolParam(description = "按处理人 ID 过滤，可选") String assigneeId,
            @ToolParam(description = "按创建人 ID 过滤，可选") String reporterId) {
        return workOrderService.search(keyword, status, priority, assigneeId, reporterId);
    }
}
