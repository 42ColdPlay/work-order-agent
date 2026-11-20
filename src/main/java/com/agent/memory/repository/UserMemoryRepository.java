package com.agent.memory.repository;

import com.agent.memory.entity.MemoryStatus;
import com.agent.memory.entity.UserMemoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserMemoryRepository extends JpaRepository<UserMemoryEntity, Long> {

    List<UserMemoryEntity> findTop50ByUserIdAndStatusOrderByCreatedAtDesc(String userId, MemoryStatus status);

    Optional<UserMemoryEntity> findByVectorId(String vectorId);

    long deleteByUserId(String userId);
}
