package com.agent.web;

import com.agent.agent.WorkOrderAgent;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final WorkOrderAgent workOrderAgent;

    public ChatController(WorkOrderAgent workOrderAgent) {
        this.workOrderAgent = workOrderAgent;
    }

    @PostMapping
    public Map<String, String> chat(@RequestBody ChatRequest request, Authentication auth) {
        String userId = resolveUserId(auth, request.userId());
        String reply = workOrderAgent.chat(userId, request.message());
        return Map.of("reply", reply);
    }

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatStream(@RequestBody ChatRequest request, Authentication auth) {
        String userId = resolveUserId(auth, request.userId());
        return workOrderAgent.chatStream(userId, request.message())
                .concatWithValues("[DONE]");
    }

    private String resolveUserId(Authentication auth, String requestUserId) {
        // 优先从 JWT 中的 userId 取，兼容前端已有 userId 的逻辑
        if (auth != null && auth.getPrincipal() instanceof Long uid) {
            return "user_" + uid;
        }
        return requestUserId != null ? requestUserId : "anonymous";
    }

    record ChatRequest(String userId, String message) {}
}
