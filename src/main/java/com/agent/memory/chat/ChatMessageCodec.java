package com.agent.memory.chat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Message ↔ JSON 编解码。
 * <p>
 * 不用 JDK 序列化：Message 是多态接口（User/Assistant/System/Tool），
 * JDK 序列化跨版本不可靠；这里用 type 字段做显式类型标记，字段结构稳定、可跨版本回放。
 */
@Component
public class ChatMessageCodec {

    private static final String FIELD_TYPE = "type";
    private static final String FIELD_CONTENT = "content";
    private static final String FIELD_METADATA = "metadata";
    private static final String FIELD_TOOL_CALLS = "toolCalls";
    private static final String FIELD_TOOL_RESPONSES = "toolResponses";

    private final ObjectMapper objectMapper;

    public ChatMessageCodec(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String serialize(Message message) throws JsonProcessingException {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put(FIELD_TYPE, message.getMessageType().name());
        map.put(FIELD_CONTENT, message.getText() == null ? "" : message.getText());
        if (message.getMetadata() != null && !message.getMetadata().isEmpty()) {
            map.put(FIELD_METADATA, message.getMetadata());
        }

        // toolCalls 必须完整保留，否则回放时模型看不到自己发起过调用
        if (message instanceof AssistantMessage assistant && assistant.hasToolCalls()) {
            List<Map<String, String>> toolCalls = new ArrayList<>(assistant.getToolCalls().size());
            for (AssistantMessage.ToolCall call : assistant.getToolCalls()) {
                Map<String, String> item = new LinkedHashMap<>();
                item.put("id", call.id());
                item.put("type", call.type());
                item.put("name", call.name());
                item.put("arguments", call.arguments());
                toolCalls.add(item);
            }
            map.put(FIELD_TOOL_CALLS, toolCalls);
        }

        // toolResponse 同理，缺了它调用链就断了
        if (message instanceof ToolResponseMessage toolMessage) {
            List<Map<String, String>> responses = new ArrayList<>(toolMessage.getResponses().size());
            for (ToolResponseMessage.ToolResponse response : toolMessage.getResponses()) {
                Map<String, String> item = new LinkedHashMap<>();
                item.put("id", response.id());
                item.put("name", response.name());
                item.put("responseData", response.responseData());
                responses.add(item);
            }
            map.put(FIELD_TOOL_RESPONSES, responses);
        }

        return objectMapper.writeValueAsString(map);
    }

    @SuppressWarnings("unchecked")
    public Message deserialize(String json) throws JsonProcessingException {
        Map<String, Object> map = objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {
        });

        String type = String.valueOf(map.get(FIELD_TYPE));
        String content = map.get(FIELD_CONTENT) == null ? "" : map.get(FIELD_CONTENT).toString();

        Map<String, Object> metadata = map.get(FIELD_METADATA) instanceof Map<?, ?> raw
                ? new LinkedHashMap<>((Map<String, Object>) raw)
                : new LinkedHashMap<>();

        return switch (MessageType.valueOf(type)) {
            case USER -> new UserMessage(content, List.of(), metadata);
            case SYSTEM -> new SystemMessage(content);
            case ASSISTANT -> new AssistantMessage(content, metadata, parseToolCalls(map.get(FIELD_TOOL_CALLS)));
            case TOOL -> new ToolResponseMessage(parseToolResponses(map.get(FIELD_TOOL_RESPONSES)), metadata);
        };
    }

    private List<AssistantMessage.ToolCall> parseToolCalls(Object raw) {
        if (!(raw instanceof List<?> list)) {
            return List.of();
        }
        List<AssistantMessage.ToolCall> result = new ArrayList<>(list.size());
        for (Object item : list) {
            if (item instanceof Map<?, ?> map) {
                result.add(new AssistantMessage.ToolCall(
                        str(map.get("id")),
                        str(map.get("type")),
                        str(map.get("name")),
                        str(map.get("arguments"))));
            }
        }
        return result;
    }

    private List<ToolResponseMessage.ToolResponse> parseToolResponses(Object raw) {
        if (!(raw instanceof List<?> list)) {
            return List.of();
        }
        List<ToolResponseMessage.ToolResponse> result = new ArrayList<>(list.size());
        for (Object item : list) {
            if (item instanceof Map<?, ?> map) {
                result.add(new ToolResponseMessage.ToolResponse(
                        str(map.get("id")),
                        str(map.get("name")),
                        str(map.get("responseData"))));
            }
        }
        return result;
    }

    private String str(Object value) {
        return value == null ? "" : value.toString();
    }
}
