package com.agent.memory.repository;

import com.agent.memory.entity.ChatMessageEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessageEntity, Long> {

    /**
     * 倒序取最近 N 条，用于缓存击穿后重建窗口。
     */
    List<ChatMessageEntity> findByConversationIdOrderByIdDesc(String conversationId, Pageable pageable);

    long deleteByConversationId(String conversationId);
}
