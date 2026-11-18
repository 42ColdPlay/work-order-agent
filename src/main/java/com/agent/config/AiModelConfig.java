package com.agent.config;

import org.springframework.context.annotation.Configuration;

/**
 * AI 模型配置占位。
 * <p>
 * ChatClient 的构建配置已移至 WorkOrderAgent，
 * 避免 ChatClient.Builder 与 Agent 之间的循环依赖。
 */
@Configuration
public class AiModelConfig {
}