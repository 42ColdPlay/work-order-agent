package com.agent.memory.web;

import com.agent.memory.longterm.LongTermMemoryService;
import com.agent.memory.longterm.MemoryItem;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 长期记忆的查看与清理入口，便于调试和满足"可删除"要求。
 */
@RestController
@RequestMapping("/api/memory")
public class MemoryController {

    private final LongTermMemoryService memoryService;

    public MemoryController(LongTermMemoryService memoryService) {
        this.memoryService = memoryService;
    }

    /** 当前用户生效中的记忆列表 */
    @GetMapping
    public List<MemoryItem> list(Authentication auth, @RequestParam(required = false) String userId) {
        return memoryService.list(resolveUserId(auth, userId));
    }

    /** 按问题试召回，用于验证向量检索效果 */
    @GetMapping("/recall")
    public List<MemoryItem> recall(@RequestParam("q") String query,
                                   Authentication auth,
                                   @RequestParam(required = false) String userId) {
        return memoryService.recall(resolveUserId(auth, userId), query);
    }

    /** 清除当前用户的全部长期记忆 */
    @DeleteMapping
    public Map<String, Object> clear(Authentication auth, @RequestParam(required = false) String userId) {
        String resolved = resolveUserId(auth, userId);
        memoryService.clear(resolved);
        return Map.of("cleared", true, "userId", resolved);
    }

    private String resolveUserId(Authentication auth, String requestUserId) {
        if (auth != null && auth.getPrincipal() instanceof Long uid) {
            return "user_" + uid;
        }
        return requestUserId != null ? requestUserId : "anonymous";
    }
}
