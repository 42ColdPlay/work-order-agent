package com.agent.auth.mapper;

import com.agent.auth.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Optional;

@Mapper
public interface UserMapper {

    Optional<User> findByUsername(@Param("username") String username);

    int countByUsername(@Param("username") String username);

    /** 插入后自增主键回填到 user.id */
    int insert(User user);
}
