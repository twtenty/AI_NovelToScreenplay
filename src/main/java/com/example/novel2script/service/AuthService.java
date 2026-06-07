package com.example.novel2script.service;

import com.example.novel2script.model.AuthRequest;
import jakarta.annotation.PostConstruct;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class AuthService {
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_\\-]{3,20}$");
    private static final BigDecimal VIP_PRICE = new BigDecimal("10.00");
    private final JdbcTemplate jdbcTemplate;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void initializeSchema() {
        jdbcTemplate.execute("""
                create table if not exists users (
                    username varchar(20) primary key,
                    salt varchar(64) not null,
                    password_hash varchar(128) not null,
                    vip boolean not null default false,
                    balance decimal(10,2) not null default 0.00,
                    created_at timestamp default current_timestamp
                )
                """);
        try {
            jdbcTemplate.execute("alter table users add column vip boolean not null default false");
        } catch (DataAccessException ignored) {
            // Existing databases already have the column.
        }
        try {
            jdbcTemplate.execute("alter table users add column balance decimal(10,2) not null default 0.00");
        } catch (DataAccessException ignored) {
            // Existing databases already have the column.
        }
    }

    public String register(AuthRequest request) {
        validateRequest(request);
        String username = normalizeUsername(request.getUsername());
        if (existsByUsername(username)) {
            throw new IllegalArgumentException("用户名已存在。");
        }

        String salt = generateSalt();
        jdbcTemplate.update(
                "insert into users (username, salt, password_hash, vip, balance) values (?, ?, ?, ?, ?)",
                username,
                salt,
                hashPassword(request.getPassword(), salt),
                false,
                BigDecimal.ZERO
        );
        return username;
    }

    public String login(AuthRequest request) {
        validateRequest(request);
        String username = normalizeUsername(request.getUsername());
        List<StoredUser> matchedUsers = jdbcTemplate.query(
                "select username, salt, password_hash, vip from users where username = ?",
                (rs, rowNum) -> new StoredUser(
                        rs.getString("username"),
                        rs.getString("salt"),
                        rs.getString("password_hash"),
                        rs.getBoolean("vip")
                ),
                username
        );

        if (matchedUsers.isEmpty()) {
            throw new IllegalArgumentException("用户名或密码错误。");
        }

        StoredUser user = matchedUsers.get(0);
        if (!user.passwordHash.equals(hashPassword(request.getPassword(), user.salt))) {
            throw new IllegalArgumentException("用户名或密码错误。");
        }
        return user.username;
    }

    public boolean isVip(String username) {
        String normalizedUsername = normalizeUsername(username);
        if (normalizedUsername.isEmpty()) {
            return false;
        }
        Boolean vip = jdbcTemplate.queryForObject(
                "select vip from users where username = ?",
                Boolean.class,
                normalizedUsername
        );
        return Boolean.TRUE.equals(vip);
    }

    public BigDecimal getBalance(String username) {
        String normalizedUsername = normalizeUsername(username);
        if (normalizedUsername.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal balance = jdbcTemplate.queryForObject(
                "select balance from users where username = ?",
                BigDecimal.class,
                normalizedUsername
        );
        return balance == null ? BigDecimal.ZERO : balance;
    }

    @Transactional
    public BigDecimal recharge(String username, BigDecimal amount) {
        String normalizedUsername = normalizeUsername(username);
        if (normalizedUsername.isEmpty()) {
            throw new IllegalArgumentException("用户未登录。");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("充值金额必须大于 0 元。");
        }
        if (amount.compareTo(new BigDecimal("9999.00")) > 0) {
            throw new IllegalArgumentException("单次充值金额不能超过 9999 元。");
        }
        int updatedRows = jdbcTemplate.update("update users set balance = balance + ? where username = ?", amount, normalizedUsername);
        if (updatedRows == 0) {
            throw new IllegalArgumentException("用户不存在。");
        }
        return getBalance(normalizedUsername);
    }

    @Transactional
    public boolean upgradeToVip(String username) {
        String normalizedUsername = normalizeUsername(username);
        if (normalizedUsername.isEmpty()) {
            throw new IllegalArgumentException("用户未登录。");
        }
        if (isVip(normalizedUsername)) {
            return true;
        }
        BigDecimal balance = getBalance(normalizedUsername);
        if (balance.compareTo(VIP_PRICE) < 0) {
            throw new IllegalArgumentException("余额不足 10 元，请先充值。");
        }
        int updatedRows = jdbcTemplate.update(
                "update users set vip = true, balance = balance - ? where username = ?",
                VIP_PRICE,
                normalizedUsername
        );
        if (updatedRows == 0) {
            throw new IllegalArgumentException("用户不存在。");
        }
        return true;
    }

    private boolean existsByUsername(String username) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from users where username = ?",
                Integer.class,
                username
        );
        return count != null && count > 0;
    }

    private void validateRequest(AuthRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("请求不能为空。");
        }
        String username = normalizeUsername(request.getUsername());
        if (!USERNAME_PATTERN.matcher(username).matches()) {
            throw new IllegalArgumentException("用户名需为 3-20 位字母、数字、下划线或短横线。");
        }
        if (request.getPassword() == null || request.getPassword().length() < 6) {
            throw new IllegalArgumentException("密码至少需要 6 位。");
        }
    }

    private String normalizeUsername(String username) {
        return username == null ? "" : username.trim();
    }

    private String generateSalt() {
        byte[] salt = new byte[16];
        secureRandom.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    private String hashPassword(String password, String salt) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest((salt + ":" + password).getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("当前 Java 环境不支持 SHA-256。", e);
        }
    }

    record StoredUser(String username, String salt, String passwordHash, boolean vip) {
    }
}
