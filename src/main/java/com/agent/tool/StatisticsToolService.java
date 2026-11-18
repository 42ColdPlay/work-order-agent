package com.agent.tool;

import com.agent.wo.entity.WorkOrderStatus;
import com.agent.wo.service.WorkOrderMockService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 统计报表相关工具。
 */
@Service
public class StatisticsToolService {

    private final WorkOrderMockService workOrderService;

    public StatisticsToolService(WorkOrderMockService workOrderService) {
        this.workOrderService = workOrderService;
    }

    @Tool(description = "按状态统计工单数量，返回各状态的工单计数")
    public String getWorkOrderStatistics() {
        Map<WorkOrderStatus, Long> stats = workOrderService.getStatistics();
        StringBuilder sb = new StringBuilder("工单统计：\n");
        for (Map.Entry<WorkOrderStatus, Long> entry : stats.entrySet()) {
            sb.append("  ").append(entry.getKey().getDisplayName())
                    .append("：").append(entry.getValue()).append(" 条\n");
        }
        return sb.toString();
    }

    @Tool(description = "按优先级统计工单数量")
    public String getPriorityStatistics() {
        Map<String, Long> stats = workOrderService.getPriorityStatistics();
        StringBuilder sb = new StringBuilder("按优先级统计：\n");
        for (Map.Entry<String, Long> entry : stats.entrySet()) {
            sb.append("  ").append(entry.getKey()).append("：").append(entry.getValue()).append(" 条\n");
        }
        return sb.toString();
    }
}
