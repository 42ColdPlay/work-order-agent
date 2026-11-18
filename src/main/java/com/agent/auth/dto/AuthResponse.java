package com.agent.auth.dto;

public class AuthResponse {
    private String token;
    private String username;
    private String displayName;
    private String role;

    public AuthResponse(String token, String username, String displayName, String role) {
        this.token = token;
        this.username = username;
        this.displayName = displayName;
        this.role = role;
    }

    public String getToken() { return token; }
    public String getUsername() { return username; }
    public String getDisplayName() { return displayName; }
    public String getRole() { return role; }
}