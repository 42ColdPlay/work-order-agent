package com.agent.memory.longterm;

import com.agent.memory.MemoryProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;

/**
 * 长期记忆抽取器。
 * <p>
 * 每轮对话结束后异步调用一次 LLM，从"用户问了什么 + 助手答了什么"里提炼稳定事实。
 * 抽取用的 ChatClient 不带任何 advisor，避免与主对话的记忆 advisor 形成递归。
 * <p>
 * 全链路异常只记日志：记忆是增强能力，不能因为抽取失败影响正常对话。
 */
@Component
public class MemoryExtractor {

    private static final Logger log = LoggerFactory.getLogger(MemoryExtractor.class);

    private static final String EXTRACTION_PROMPT = """
            你是记忆抽取器。请从下面的对话中抽取关于"用户本人"的、值得长期记住的事实。

            抽取规则：
            1. 只抽取稳定信息：身份/部门/岗位、使用的设备与系统、历史工单及处理结论、明确的偏好或习惯
            2. 忽略一次性内容：问候、客套、临时查询条件、与用户无关的通用知识
            3. 每条事实必须独立成句、可脱离上下文阅读，不要出现"用户说""刚才"等指代
            4. 合并同义表述，最多输出 5 条；没有值得记住的内容就输出 []

            严格只输出 JSON 数组，不要解释、不要 markdown 代码块：
            [{"content":"事实内容","category":"profile|preference|history|other","importance":1-10}]
            """;

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;
    private final LongTermMemoryService memoryService;
    private final MemoryProperties properties;
    private final ExecutorService executor;

    public MemoryExtractor(ChatClient.Builder chatClientBuilder,
                           ObjectMapper objectMapper,
                           LongTermMemoryService memoryService,
                           MemoryProperties properties,
                           @Qualifier("memoryExecutor") ExecutorService executor) {
        this.chatClient = chatClientBuilder.build();
        this.objectMapper = objectMapper;
        this.memoryService = memoryService;
        this.properties = properties;
        this.executor = executor;
    }

    /**
     * 异步抽取，调用方无需关心结果。
     *
     * @param userId           用户 ID，同时作为记忆归属
     * @param conversationId   来源会话，便于追溯
     * @param userMessage      本轮用户输入
     * @param assistantReply   本轮助手回复
     */
    public void extractAsync(String userId, String conversationId, String userMessage, String assistantReply) {
        MemoryProperties.LongTerm config = properties.getLongterm();
        if (!config.isEnabled() || !config.isExtractionEnabled()) {
            return;
        }
        if (userMessage == null || userMessage.trim().length() < config.getMinMessageLength()) {
            return;
        }
        if (assistantReply == null || assistantReply.isBlank()) {
            return;
        }

        String user = truncate(userMessage, 2000);
        String reply = truncate(assistantReply, 2000);
        try {
            executor.submit(() -> doExtract(userId, conversationId, user, reply));
        } catch (RejectedExecutionException e) {
            log.warn("记忆抽取任务被拒绝（线程池已满）: {}", e.getMessage());
        }
    }

    private void doExtract(String userId, String conversationId, String userMessage, String assistantReply) {
        long start = System.currentTimeMillis();
        try {
            String raw = chatClient.prompt()
                    .system(EXTRACTION_PROMPT)
                    .user("用户：" + userMessage + "\n助手：" + assistantReply)
                    .call()
                    .content();

            List<ExtractedFact> facts = parse(raw);
            for (ExtractedFact fact : facts) {
                memoryService.save(userId, fact.content(), fact.category(), fact.importance(), conversationId);
            }
            log.info("记忆抽取完成: user={}, 候选 {} 条, 耗时 {}ms",
                    userId, facts.size(), System.currentTimeMillis() - start);
        } catch (Exception e) {
            log.warn("记忆抽取失败（不影响主流程）: {}", e.getMessage());
        }
    }

    private List<ExtractedFact> parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }

        int start = raw.indexOf('[');
        int end = raw.lastIndexOf(']');
        if (start < 0 || end <= start) {
            log.debug("记忆抽取结果不是 JSON 数组，忽略: {}", truncate(raw, 200));
            return List.of();
        }

        String json = raw.substring(start, end + 1);
        List<Map<String, Object>> list;
        try {
            list = objectMapper.readValue(json, new TypeReference<List<Map<String, Object>>>() {
            });
        } catch (Exception e) {
            log.warn("记忆抽取结果解析失败: {}", e.getMessage());
            return List.of();
        }

        int limit = Math.max(1, properties.getLongterm().getMaxFactsPerTurn());
        List<ExtractedFact> facts = new ArrayList<>();
        for (Map<String, Object> item : list) {
            if (facts.size() >= limit) {
                break;
            }
            Object content = item.get("content");
            if (content == null) {
                continue;
            }
            String text = content.toString().trim();
            if (text.length() < 4) {
                continue;
            }
            facts.add(new ExtractedFact(
                    truncate(text, 300),
                    str(item.get("category")),
                    intOf(item.get("importance"))));
        }
        return facts;
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private String str(Object value) {
        return value == null ? "other" : value.toString();
    }

    private int intOf(Object value) {
        if (value instanceof Number number) {
            return Math.max(1, Math.min(10, number.intValue()));
        }
        try {
            return Math.max(1, Math.min(10, Integer.parseInt(String.valueOf(value))));
        } catch (NumberFormatException e) {
            return 5;
        }
    }

    private record ExtractedFact(String content, String category, int importance) {
    }
}
