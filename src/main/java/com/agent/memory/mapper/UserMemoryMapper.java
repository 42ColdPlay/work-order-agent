package com.agent.memory.mapper;

import com.agent.memory.entity.MemoryStatus;
import com.agent.memory.entity.UserMemoryEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface UserMemoryMapper {

    /** 插入后自增主键回填到 entity.id */
    int insert(UserMemoryEntity entity);

    List<UserMemoryEntity> findActiveByUserId(@Param("userId") String userId,
                                              @Param("status") MemoryStatus status,
                                              @Param("limit") int limit);

    Optional<UserMemoryEntity> findByVectorId(@Param("vectorId") String vectorId);

    int updateStatus(@Param("id") Long id, @Param("status") MemoryStatus status);

    int deleteByUserId(@Param("userId") String userId);
}
