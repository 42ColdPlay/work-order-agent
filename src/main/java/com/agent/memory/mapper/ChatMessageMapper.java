package com.agent.memory.mapper;

import com.agent.memory.entity.ChatMessageEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ChatMessageMapper {

    int insertBatch(@Param("rows") List<ChatMessageEntity> rows);

    /**
     * 倒序取最近 N 条，用于缓存击穿后重建窗口。
     */
    List<ChatMessageEntity> findRecent(@Param("conversationId") String conversationId,
                                       @Param("limit") int limit);

    int deleteByConversationId(@Param("conversationId") String conversationId);
}
