package com.example.novel2script.service;

import com.example.novel2script.model.AuthRequest;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceTest {
    @Test
    void registersUserIntoDatabase() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForObject(any(String.class), eq(Integer.class), eq("writer01"))).thenReturn(0);

        AuthService authService = new AuthService(jdbcTemplate);
        AuthRequest request = request("writer01", "secret123");

        assertEquals("writer01", authService.register(request));
        verify(jdbcTemplate).update(any(String.class), eq("writer01"), any(String.class), any(String.class), eq(false), eq(BigDecimal.ZERO));
    }

    @Test
    void rejectsDuplicateUsername() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForObject(any(String.class), eq(Integer.class), eq("writer01"))).thenReturn(1);

        AuthService authService = new AuthService(jdbcTemplate);

        assertThrows(IllegalArgumentException.class, () -> authService.register(request("writer01", "secret123")));
    }

    @Test
    void logsInUserFromDatabase() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        AuthService authService = new AuthService(jdbcTemplate);

        when(jdbcTemplate.query(any(String.class), any(RowMapper.class), eq("writer01")))
                .thenReturn(List.of(new AuthService.StoredUser(
                        "writer01",
                        "test-salt",
                        hashPassword(authService, "secret123", "test-salt"),
                        false
                )));

        assertEquals("writer01", authService.login(request("writer01", "secret123")));
    }

    @Test
    void rejectsWrongPassword() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        AuthService authService = new AuthService(jdbcTemplate);

        when(jdbcTemplate.query(any(String.class), any(RowMapper.class), eq("writer01")))
                .thenReturn(List.of(new AuthService.StoredUser("writer01", "test-salt", "bad-hash", false)));

        assertThrows(IllegalArgumentException.class, () -> authService.login(request("writer01", "bad-password")));
    }

    @Test
    void checksVipStatusFromDatabase() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForObject(any(String.class), eq(Boolean.class), eq("writer01"))).thenReturn(true);

        AuthService authService = new AuthService(jdbcTemplate);

        assertEquals(true, authService.isVip("writer01"));
    }

    @Test
    void upgradesUserToVip() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForObject(any(String.class), eq(Boolean.class), eq("writer01"))).thenReturn(false);
        when(jdbcTemplate.queryForObject(any(String.class), eq(BigDecimal.class), eq("writer01"))).thenReturn(new BigDecimal("20.00"));
        when(jdbcTemplate.update(any(String.class), eq(new BigDecimal("10.00")), eq("writer01"))).thenReturn(1);

        AuthService authService = new AuthService(jdbcTemplate);

        assertEquals(true, authService.upgradeToVip("writer01"));
        verify(jdbcTemplate).update(any(String.class), eq(new BigDecimal("10.00")), eq("writer01"));
    }

    @Test
    void rejectsVipUpgradeWhenBalanceIsInsufficient() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForObject(any(String.class), eq(Boolean.class), eq("writer01"))).thenReturn(false);
        when(jdbcTemplate.queryForObject(any(String.class), eq(BigDecimal.class), eq("writer01"))).thenReturn(new BigDecimal("9.99"));

        AuthService authService = new AuthService(jdbcTemplate);

        assertThrows(IllegalArgumentException.class, () -> authService.upgradeToVip("writer01"));
    }

    @Test
    void rechargesUserBalance() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.update(any(String.class), eq(new BigDecimal("30.00")), eq("writer01"))).thenReturn(1);
        when(jdbcTemplate.queryForObject(any(String.class), eq(BigDecimal.class), eq("writer01"))).thenReturn(new BigDecimal("30.00"));

        AuthService authService = new AuthService(jdbcTemplate);

        assertEquals(new BigDecimal("30.00"), authService.recharge("writer01", new BigDecimal("30.00")));
        verify(jdbcTemplate).update(any(String.class), eq(new BigDecimal("30.00")), eq("writer01"));
    }

    private String hashPassword(AuthService authService, String password, String salt) {
        try {
            var method = AuthService.class.getDeclaredMethod("hashPassword", String.class, String.class);
            method.setAccessible(true);
            return (String) method.invoke(authService, password, salt);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private AuthRequest request(String username, String password) {
        AuthRequest request = new AuthRequest();
        request.setUsername(username);
        request.setPassword(password);
        return request;
    }
}
