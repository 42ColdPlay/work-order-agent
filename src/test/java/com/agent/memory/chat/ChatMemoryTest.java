package com.agent.memory.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ChatMemoryTest {

    @Test
    void 丢弃开头的孤立工具消息() {
        List<Message> messages = List.of(
                new ToolResponseMessage(List.of(
                        new ToolResponseMessage.ToolResponse("call_1", "getOrder", "{\"id\":1}"))),
                new AssistantMessage("已为您查到工单"));

        List<Message> sanitized = MessageWindow.sanitize(messages);

        assertThat(sanitized).hasSize(1);
        assertThat(sanitized.get(0).getMessageType()).isEqualTo(MessageType.ASSISTANT);
    }

    @Test
    void 丢弃结尾没有响应的工具调用() {
        AssistantMessage incomplete = new AssistantMessage("正在查询", java.util.Map.of(),
                List.of(new AssistantMessage.ToolCall("call_1", "function", "getOrder", "{}")));
        List<Message> messages = List.of(new UserMessage("查一下工单 1"), incomplete);

        List<Message> sanitized = MessageWindow.sanitize(messages);

        assertThat(sanitized).hasSize(1);
        assertThat(sanitized.get(0).getMessageType()).isEqualTo(MessageType.USER);
    }

    @Test
    void 超出token预算时保留最新消息() {
        List<Message> messages = List.of(
                new UserMessage("旧".repeat(100)),
                new UserMessage("中".repeat(100)),
                new UserMessage("新".repeat(10)));

        List<Message> window = MessageWindow.byTokenBudget(messages, 10);

        assertThat(window).hasSize(1);
        assertThat(window.get(0).getText()).startsWith("新");
    }

    @Test
    void 工具调用可以完整往返() throws Exception {
        ChatMessageCodec codec = new ChatMessageCodec(new ObjectMapper());

        AssistantMessage original = new AssistantMessage(
                "",
                java.util.Map.of("model", "test"),
                List.of(new AssistantMessage.ToolCall("call_1", "function", "createOrder", "{\"title\":\"打印机故障\"}")));

        Message restored = codec.deserialize(codec.serialize(original));

        assertThat(restored).isInstanceOf(AssistantMessage.class);
        AssistantMessage assistant = (AssistantMessage) restored;
        assertThat(assistant.hasToolCalls()).isTrue();
        assertThat(assistant.getToolCalls().get(0).name()).isEqualTo("createOrder");
        assertThat(assistant.getToolCalls().get(0).arguments()).contains("打印机故障");
        assertThat(assistant.getMetadata()).containsEntry("model", "test");
    }
}
