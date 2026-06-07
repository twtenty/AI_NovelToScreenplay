package com.example.novel2script.model;

public class AuthResponse {
    private boolean success;
    private String username;
    private String error;

    public AuthResponse() {
    }

    public AuthResponse(boolean success, String username, String error) {
        this.success = success;
        this.username = username;
        this.error = error;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
