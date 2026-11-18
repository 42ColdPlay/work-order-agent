package com.agent.wo.service;

import com.agent.wo.entity.WorkOrder;
import com.agent.wo.entity.WorkOrderStatus;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 工单模拟服务 —— 用内存 Map 模拟工单系统。
 * <p>
 * 生产环境替换为真实的工单 Service 或 Feign 调用即可。
 */
@Service
public class WorkOrderMockService {

    private static final Logger log = LoggerFactory.getLogger(WorkOrderMockService.class);

    private final Map<String, WorkOrder> store = new ConcurrentHashMap<>();
    private final AtomicInteger idSeq = new AtomicInteger(0);

    @PostConstruct
    public void init() {
        // 预置示例数据
        create("服务器宕机排查", "生产环境服务器 192.168.1.100 出现宕机，需要紧急排查", "HIGH", "zhangsan", "lisi");
        create("数据库慢查询优化", "订单模块数据库查询响应超过 5s，需要优化索引", "MEDIUM", "zhangsan", "wangwu");
        create("办公网络故障报修", "3 楼办公区网络频繁掉线，需检查交换机", "LOW", "zhaoliu", "lisi");

        String woId = create("新增报表功能开发", "周报系统需要新增数据导出功能", "MEDIUM", "wangwu", "zhaoliu");
        WorkOrder wo = store.get(woId);
        if (wo != null) {
            wo.setStatus(WorkOrderStatus.PENDING_APPROVAL);
        }

        log.info("工单模拟数据初始化完成，共 {} 条", store.size());
    }

    public WorkOrder getById(String id) {
        return store.get(id);
    }

    public List<WorkOrder> getPendingOrders(String assigneeId) {
        return store.values().stream()
                .filter(wo -> assigneeId.equals(wo.getAssigneeId()))
                .filter(wo -> wo.getStatus() == WorkOrderStatus.PENDING
                        || wo.getStatus() == WorkOrderStatus.IN_PROGRESS)
                .sorted(Comparator.comparing(WorkOrder::getCreatedAt).reversed())
                .collect(Collectors.toList());
    }

    public List<WorkOrder> getPendingApprovals(String approverId) {
        return store.values().stream()
                .filter(wo -> wo.getStatus() == WorkOrderStatus.PENDING_APPROVAL)
                .sorted(Comparator.comparing(WorkOrder::getCreatedAt).reversed())
                .collect(Collectors.toList());
    }

    public String create(String title, String description, String priority,
                         String reporterId, String assigneeId) {
        String id = "WO-" + idSeq.incrementAndGet();
        WorkOrder wo = new WorkOrder(id, title, description,
                priority.toUpperCase(), WorkOrderStatus.PENDING,
                reporterId, assigneeId);
        store.put(id, wo);
        log.info("创建工单: {} - {}", id, title);
        return id;
    }

    public boolean reassign(String id, String newAssigneeId) {
        WorkOrder wo = store.get(id);
        if (wo == null) return false;
        wo.setAssigneeId(newAssigneeId);
        wo.setUpdatedAt(LocalDateTime.now());
        log.info("工单转派: {} → {}", id, newAssigneeId);
        return true;
    }

    public void approve(String id, String comment) {
        WorkOrder wo = store.get(id);
        if (wo == null) return;
        wo.setStatus(WorkOrderStatus.APPROVED);
        wo.setUpdatedAt(LocalDateTime.now());
        log.info("工单审批通过: {}，意见: {}", id, comment);
    }

    public void reject(String id, String reason) {
        WorkOrder wo = store.get(id);
        if (wo == null) return;
        wo.setStatus(WorkOrderStatus.REJECTED);
        wo.setUpdatedAt(LocalDateTime.now());
        log.info("工单驳回: {}，原因: {}", id, reason);
    }

    public String getProgress(String id) {
        WorkOrder wo = store.get(id);
        if (wo == null) return "工单不存在";
        return switch (wo.getStatus()) {
            case PENDING -> "工单 [" + id + "] 待处理，当前处理人：" + wo.getAssigneeId();
            case IN_PROGRESS -> "工单 [" + id + "] 处理中，当前节点：实施中";
            case PENDING_APPROVAL -> "工单 [" + id + "] 待审批，等待审批人确认";
            case APPROVED -> "工单 [" + id + "] 已审批通过，流程结束";
            case REJECTED -> "工单 [" + id + "] 已驳回，等待重新提交";
            case COMPLETED -> "工单 [" + id + "] 已完成";
            case CLOSED -> "工单 [" + id + "] 已关闭";
        };
    }

    public Map<WorkOrderStatus, Long> getStatistics() {
        return store.values().stream()
                .collect(Collectors.groupingBy(WorkOrder::getStatus, Collectors.counting()));
    }

    public Map<String, Long> getPriorityStatistics() {
        return store.values().stream()
                .collect(Collectors.groupingBy(WorkOrder::getPriority, Collectors.counting()));
    }

    /**
     * 按条件搜索工单（关键词模糊匹配 + 多条件过滤）。
     */
    public List<WorkOrder> search(String keyword, String status, String priority,
                                  String assigneeId, String reporterId) {
        return store.values().stream()
                .filter(wo -> {
                    if (keyword == null || keyword.isBlank()) return true;
                    String kw = keyword.toLowerCase();
                    return wo.getTitle().toLowerCase().contains(kw)
                            || wo.getDescription().toLowerCase().contains(kw);
                })
                .filter(wo -> status == null || status.isBlank()
                        || wo.getStatus().name().equalsIgnoreCase(status))
                .filter(wo -> priority == null || priority.isBlank()
                        || wo.getPriority().equalsIgnoreCase(priority))
                .filter(wo -> assigneeId == null || assigneeId.isBlank()
                        || assigneeId.equals(wo.getAssigneeId()))
                .filter(wo -> reporterId == null || reporterId.isBlank()
                        || reporterId.equals(wo.getReporterId()))
                .sorted(Comparator.comparing(WorkOrder::getCreatedAt).reversed())
                .collect(Collectors.toList());
    }
}
