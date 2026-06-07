package com.example.novel2script.model;

import java.math.BigDecimal;

public class AuthResponse {
    private boolean success;
    private String username;
    private boolean vip;
    private BigDecimal balance = BigDecimal.ZERO;
    private String error;

    public AuthResponse() {
    }

    public AuthResponse(boolean success, String username, String error) {
        this(success, username, false, BigDecimal.ZERO, error);
    }

    public AuthResponse(boolean success, String username, boolean vip, String error) {
        this(success, username, vip, BigDecimal.ZERO, error);
    }

    public AuthResponse(boolean success, String username, boolean vip, BigDecimal balance, String error) {
        this.success = success;
        this.username = username;
        this.vip = vip;
        this.balance = balance == null ? BigDecimal.ZERO : balance;
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

    public boolean isVip() {
        return vip;
    }

    public void setVip(boolean vip) {
        this.vip = vip;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance == null ? BigDecimal.ZERO : balance;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
