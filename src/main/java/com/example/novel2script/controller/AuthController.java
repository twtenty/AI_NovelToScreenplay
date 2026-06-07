package com.example.novel2script.controller;

import com.example.novel2script.model.AuthRequest;
import com.example.novel2script.model.AuthResponse;
import com.example.novel2script.model.RechargeRequest;
import com.example.novel2script.service.AuthService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    public static final String SESSION_USERNAME = "username";
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public AuthResponse register(@RequestBody AuthRequest request, HttpSession session) {
        try {
            String username = authService.register(request);
            session.setAttribute(SESSION_USERNAME, username);
            return authResponse(username);
        } catch (Exception e) {
            return new AuthResponse(false, null, e.getMessage());
        }
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody AuthRequest request, HttpSession session) {
        try {
            String username = authService.login(request);
            session.setAttribute(SESSION_USERNAME, username);
            return authResponse(username);
        } catch (Exception e) {
            return new AuthResponse(false, null, e.getMessage());
        }
    }

    @GetMapping("/me")
    public AuthResponse currentUser(HttpSession session) {
        String username = (String) session.getAttribute(SESSION_USERNAME);
        return username == null ? new AuthResponse(false, null, null) : authResponse(username);
    }

    @PostMapping("/vip")
    public AuthResponse upgradeToVip(HttpSession session) {
        String username = (String) session.getAttribute(SESSION_USERNAME);
        if (username == null) {
            return new AuthResponse(false, null, false, "请先登录后再充值会员。");
        }
        try {
            authService.upgradeToVip(username);
            return authResponse(username);
        } catch (Exception e) {
            return new AuthResponse(false, username, authService.isVip(username), authService.getBalance(username), e.getMessage());
        }
    }

    @PostMapping("/recharge")
    public AuthResponse recharge(@RequestBody RechargeRequest request, HttpSession session) {
        String username = (String) session.getAttribute(SESSION_USERNAME);
        if (username == null) {
            return new AuthResponse(false, null, false, "请先登录后再充值。");
        }
        try {
            authService.recharge(username, request == null ? null : request.getAmount());
            return authResponse(username);
        } catch (Exception e) {
            return new AuthResponse(false, username, authService.isVip(username), authService.getBalance(username), e.getMessage());
        }
    }

    @PostMapping("/logout")
    public AuthResponse logout(HttpSession session) {
        session.invalidate();
        return new AuthResponse(true, null, null);
    }

    private AuthResponse authResponse(String username) {
        return new AuthResponse(true, username, authService.isVip(username), authService.getBalance(username), null);
    }
}
