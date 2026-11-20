package com.agent.agent;

import com.agent.memory.chat.HybridChatMemory;
import com.agent.memory.longterm.LongTermMemoryService;
import com.agent.memory.longterm.MemoryExtractor;
import com.agent.memory.longterm.MemoryItem;
import com.agent.rag.RAGPipeline;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * 工单助手 Agent —— 核心编排器。
 * <p>
 * 编排链路：
 * <pre>
 * 用户输入 → 短期记忆（Redis + MySQL 窗口）
 *         → 长期记忆召回（向量库，用户画像/历史事实）
 *         → RAG 知识库检索
 *         → System Prompt 组装 → LLM 推理 → 输出
 *         → 异步记忆抽取（写回长期记忆）
 * </pre>
 */
@Service
public class WorkOrderAgent {

    private static final Logger log = LoggerFactory.getLogger(WorkOrderAgent.class);

    /**
     * 交给 ChatMemory 的候选条数。真正的上限由 token 预算控制，
     * 这里放宽到 40 是为了让 token 裁剪有足够的素材可用。
     */
    private static final int MEMORY_RETRIEVE_SIZE = 40;

    private final ChatClient chatClient;
    private final RAGPipeline ragPipeline;
    private final LongTermMemoryService longTermMemoryService;
    private final MemoryExtractor memoryExtractor;

    public WorkOrderAgent(ChatClient.Builder chatClientBuilder,
                          RAGPipeline ragPipeline,
                          HybridChatMemory chatMemory,
                          LongTermMemoryService longTermMemoryService,
                          MemoryExtractor memoryExtractor) {
        this.chatClient = chatClientBuilder
                .defaultAdvisors(
                        new SimpleLoggerAdvisor(),
                        new MessageChatMemoryAdvisor(chatMemory))
                .build();
        this.ragPipeline = ragPipeline;
        this.longTermMemoryService = longTermMemoryService;
        this.memoryExtractor = memoryExtractor;
    }

    /**
     * 非流式对话 —— 返回完整结果
     */
    public String chat(String userId, String message) {
        String systemText = buildSystemText(userId, message);

        String reply = chatClient.prompt()
                .user(message)
                .system(systemText)
                .advisors(a -> a
                        .param(AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY, userId)
                        .param(AbstractChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY, MEMORY_RETRIEVE_SIZE))
                .call()
                .content();

        memoryExtractor.extractAsync(userId, userId, message, reply);
        return reply;
    }

    /**
     * 流式对话 —— SSE 打字机效果
     */
    public Flux<String> chatStream(String userId, String message) {
        String systemText = buildSystemText(userId, message);
        StringBuilder replyBuffer = new StringBuilder();

        return chatClient.prompt()
                .user(message)
                .system(systemText)
                .advisors(a -> a
                        .param(AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY, userId)
                        .param(AbstractChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY, MEMORY_RETRIEVE_SIZE))
                .stream()
                .content()
                .map(chunk -> {
                    replyBuffer.append(chunk);
                    return chunk;
                })
                .doOnComplete(() -> memoryExtractor.extractAsync(userId, userId, message, replyBuffer.toString()))
                .doOnError(e -> log.warn("流式对话异常，本轮不做记忆抽取: {}", e.getMessage()));
    }

    /**
     * 组装 system prompt：角色设定 + 用户长期记忆 + RAG 上下文。
     * <p>
     * 这里在 Java 侧完成渲染后直接传纯文本（而不是带 {context} 占位符的模板），
     * 避免记忆内容里出现的花括号被当成模板变量解析。
     */
    private String buildSystemText(String userId, String message) {
        String context = ragPipeline.retrieve(message);

        List<MemoryItem> memories = longTermMemoryService.recall(userId, message);
        String memoryBlock = longTermMemoryService.formatForPrompt(memories);

        String ragInstruction = (context == null || context.isBlank())
                ? PromptTemplates.RAG_EMPTY_TEMPLATE
                : PromptTemplates.RAG_TEMPLATE.replace("{context}", context);

        StringBuilder systemText = new StringBuilder(PromptTemplates.SYSTEM_PROMPT);
        if (!memoryBlock.isBlank()) {
            systemText.append("\n\n").append(memoryBlock);
        }
        systemText.append("\n\n").append(ragInstruction);
        return systemText.toString();
    }
}
