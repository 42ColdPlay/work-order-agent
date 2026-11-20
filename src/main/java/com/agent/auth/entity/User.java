package com.agent.auth.entity;

import java.time.LocalDateTime;

/**
 * 用户表（users）实体，字段与表结构一一对应，由 MyBatis 映射。
 */
public class User {

    private Long id;

    private String username;

    private String password;

    private String displayName;

    private String role = "USER";

    private LocalDateTime createdAt = LocalDateTime.now();

    public User() {}

    public User(String username, String password, String displayName) {
        this.username = username;
        this.password = password;
        this.displayName = displayName;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}