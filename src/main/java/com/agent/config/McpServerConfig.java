package com.agent.config;

import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbacks;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

/**
 * MCP / Tool 工具注册配置。
 * <p>
 * 所有 @Tool 方法通过 ToolCallbacks 自动扫描并注册，
 * ChatClient 在推理时会根据用户问题自动决定是否调用。
 */
@Configuration
public class McpServerConfig {

    @Bean
    public List<ToolCallback> workOrderTools(
            com.agent.tool.WorkOrderToolService workOrderToolService,
            com.agent.tool.ApprovalToolService approvalToolService,
            com.agent.tool.StatisticsToolService statisticsToolService) {
        return Arrays.asList(ToolCallbacks.from(workOrderToolService, approvalToolService, statisticsToolService));
    }
}
