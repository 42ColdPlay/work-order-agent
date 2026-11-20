package com.agent.memory.chat;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.ToolResponseMessage;

import java.util.ArrayList;
import java.util.List;

/**
 * 消息窗口工具。
 * <p>
 * 负责两件事：
 * 1. 按 token 预算裁剪（保留最近的消息），避免把整段历史塞进模型
 * 2. 清理结构上不完整的工具调用消息，否则 OpenAI 兼容接口会返回 400
 */
public final class MessageWindow {

    private MessageWindow() {
    }

    /**
     * 粗略估算 token 数。
     * <p>
     * 中文约 1.5 字符/token，英文与 JSON 约 4 字符/token，取 2 作为折中值，
     * 只用于窗口裁剪，不追求精确。
     */
    public static int estimateTokens(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        return Math.max(1, text.length() / 2);
    }

    public static int estimateTokens(Message message) {
        if (message == null) {
            return 0;
        }
        // 每条消息有固定的角色/分隔开销
        int tokens = 4 + estimateTokens(message.getText());
        if (message instanceof AssistantMessage assistant && assistant.hasToolCalls()) {
            for (AssistantMessage.ToolCall call : assistant.getToolCalls()) {
                tokens += estimateTokens(call.name()) + estimateTokens(call.arguments());
            }
        }
        if (message instanceof ToolResponseMessage toolResponse) {
            for (ToolResponseMessage.ToolResponse response : toolResponse.getResponses()) {
                tokens += estimateTokens(response.responseData());
            }
        }
        return tokens;
    }

    /**
     * 从最新消息向前累加，直到超出预算。
     * <p>
     * 单条消息本身就超预算时仍然保留它——宁可超一点，也不能给模型一个空历史。
     */
    public static List<Message> byTokenBudget(List<Message> messages, int tokenBudget) {
        if (messages == null || messages.isEmpty()) {
            return List.of();
        }
        if (tokenBudget <= 0) {
            return new ArrayList<>(messages);
        }

        int total = 0;
        int start = messages.size();
        for (int i = messages.size() - 1; i >= 0; i--) {
            int cost = estimateTokens(messages.get(i));
            if (total + cost > tokenBudget && start < messages.size()) {
                break;
            }
            total += cost;
            start = i;
        }
        return new ArrayList<>(messages.subList(start, messages.size()));
    }

    /**
     * 修复被窗口截断的对话结构：
     * - 开头不能是孤立的 TOOL 消息（它对外的 assistant toolCall 已被截掉）
     * - 结尾不能是带 toolCalls 但没有 toolResponse 的 assistant 消息（流式回答中断）
     */
    public static List<Message> sanitize(List<Message> messages) {
        if (messages == null || messages.isEmpty()) {
            return List.of();
        }

        List<Message> result = new ArrayList<>(messages);
        while (!result.isEmpty() && result.get(0).getMessageType() == MessageType.TOOL) {
            result.remove(0);
        }
        while (!result.isEmpty()) {
            Message last = result.get(result.size() - 1);
            if (last instanceof AssistantMessage assistant && assistant.hasToolCalls()) {
                result.remove(result.size() - 1);
            } else {
                break;
            }
        }
        return result;
    }

    /** 先按 token 裁剪，再修复结构 */
    public static List<Message> apply(List<Message> messages, int tokenBudget) {
        return sanitize(byTokenBudget(messages, tokenBudget));
    }
}
