package com.agent.wo.controller;

import com.agent.wo.entity.WorkOrder;
import com.agent.wo.service.WorkOrderMockService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 工单系统 REST API（模拟）。
 * <p>
 * 在实际项目中，这是一个真实的工单系统后端。
 * 当前版本用内存模拟，供 Agent 的 @Tool 调用。
 */
@RestController
@RequestMapping("/api/work-orders")
public class WorkOrderController {

    private final WorkOrderMockService workOrderService;

    public WorkOrderController(WorkOrderMockService workOrderService) {
        this.workOrderService = workOrderService;
    }

    @GetMapping("/{id}")
    public WorkOrder getById(@PathVariable String id) {
        return workOrderService.getById(id);
    }

    @GetMapping("/pending/{assigneeId}")
    public List<WorkOrder> getPending(@PathVariable String assigneeId) {
        return workOrderService.getPendingOrders(assigneeId);
    }

    @PostMapping
    public String create(@RequestBody CreateRequest req) {
        return workOrderService.create(req.title, req.description, req.priority, req.reporterId, req.assigneeId);
    }

    @PostMapping("/{id}/reassign")
    public boolean reassign(@PathVariable String id, @RequestParam String newAssigneeId) {
        return workOrderService.reassign(id, newAssigneeId);
    }

    @PostMapping("/{id}/approve")
    public void approve(@PathVariable String id, @RequestParam String comment) {
        workOrderService.approve(id, comment);
    }

    @PostMapping("/{id}/reject")
    public void reject(@PathVariable String id, @RequestParam String reason) {
        workOrderService.reject(id, reason);
    }

    record CreateRequest(String title, String description, String priority,
                         String reporterId, String assigneeId) {}
}
