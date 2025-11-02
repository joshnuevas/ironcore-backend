package com.ironcore.ironcorebackend.entity;

public class LoginResponse {
    private String token;
    private Long userId;
    private String username;
    private String email;
    private String message;

    public LoginResponse(String token, Long userId, String username, String email, String message) {
        this.token = token;
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.message = message;
    }

    // Getters and Setters
    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}