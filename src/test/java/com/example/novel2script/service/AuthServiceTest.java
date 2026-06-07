package com.example.novel2script.service;

import com.example.novel2script.model.AuthRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuthServiceTest {
    @TempDir
    Path tempDir;

    @Test
    void registersAndLogsInUser() throws Exception {
        AuthService authService = new AuthService(tempDir.resolve("users.json").toString());
        AuthRequest request = request("writer01", "secret123");

        assertEquals("writer01", authService.register(request));
        assertEquals("writer01", authService.login(request));
    }

    @Test
    void rejectsDuplicateUsername() throws Exception {
        AuthService authService = new AuthService(tempDir.resolve("users.json").toString());
        AuthRequest request = request("writer01", "secret123");

        authService.register(request);

        assertThrows(IllegalArgumentException.class, () -> authService.register(request));
    }

    private AuthRequest request(String username, String password) {
        AuthRequest request = new AuthRequest();
        request.setUsername(username);
        request.setPassword(password);
        return request;
    }
}
