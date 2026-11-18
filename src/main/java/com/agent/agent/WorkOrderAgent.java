package com.agent.agent;

import com.agent.config.redis.RedisChatMemory;
import com.agent.rag.RAGPipeline;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

/**
 * 工单助手 Agent —— 核心编排器。
 * <p>
 * 职责：接收用户请求 → 调用大模型推理 → 自动触发 Tool 调用 → 返回结果
 * <p>
 * 编排链路：
 * <pre>
 * 用户输入 → System Prompt (角色设定)
 *         → 对话记忆 (上下文保持)
 *         → 工具注册 (@Tool 方法)
 *         → RAG 增强 (知识库检索)
 *         → LLM 推理 → 输出
 * </pre>
 */
@Service
public class WorkOrderAgent {

    private final ChatClient chatClient;
    private final RAGPipeline ragPipeline;

    public WorkOrderAgent(ChatClient.Builder chatClientBuilder, RAGPipeline ragPipeline, RedisChatMemory redisChatMemory) {
        // MessageChatMemoryAdvisor: 基于窗口的对话记忆，保留最近 20 轮
        this.chatClient = chatClientBuilder
                .defaultSystem("""
                        你是智能工单助手，可以处理工单相关的各种操作。

                        核心能力：
                        1. 查询工单状态、详情、进度
                        2. 创建、分配、转派工单
                        3. 审批流转、查询待办
                        4. 统计报表、数据分析
                        5. 根据知识库文档回答工单处理相关问题

                        约束：
                        - 如果用户要求执行操作（创建/审批/转派），先确认再执行
                        - 如果知识库中找不到答案，如实告知，不要编造
                        """)
                .defaultAdvisors(
                        new SimpleLoggerAdvisor(),
                        new MessageChatMemoryAdvisor(redisChatMemory))
                .build();
        this.ragPipeline = ragPipeline;
    }

    /**
     * 非流式对话 —— 返回完整结果
     */
    public String chat(String userId, String message) {
        String context = ragPipeline.retrieve(message);

        return chatClient.prompt()
                .user(u -> u.text(message))
                .system(s -> s.text("""
                        以下是知识库中可能与当前问题相关的参考信息：
                        =====
                        {context}
                        =====
                        请基于以上信息回答用户问题。如果参考信息不相关，忽略即可。
                        """)
                        .param("context", context))
                .advisors(a -> a.param("chat_memory_conversation_id", userId))
                .call()
                .content();
    }

    /**
     * 流式对话 —— SSE 打字机效果
     */
    public Flux<String> chatStream(String userId, String message) {
        String context = ragPipeline.retrieve(message);

        return chatClient.prompt()
                .user(u -> u.text(message))
                .system(s -> s.text("""
                        以下是知识库中可能与当前问题相关的参考信息：
                        =====
                        {context}
                        =====
                        """)
                        .param("context", context))
                .advisors(a -> a.param("chat_memory_conversation_id", userId))
                .stream()
                .content();
    }
}
