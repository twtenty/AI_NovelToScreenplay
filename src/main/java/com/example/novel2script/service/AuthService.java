package com.example.novel2script.service;

import com.example.novel2script.model.AuthRequest;
import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

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
                    created_at timestamp default current_timestamp
                )
                """);
    }

    public String register(AuthRequest request) {
        validateRequest(request);
        String username = normalizeUsername(request.getUsername());
        if (existsByUsername(username)) {
            throw new IllegalArgumentException("用户名已存在。");
        }

        String salt = generateSalt();
        jdbcTemplate.update(
                "insert into users (username, salt, password_hash) values (?, ?, ?)",
                username,
                salt,
                hashPassword(request.getPassword(), salt)
        );
        return username;
    }

    public String login(AuthRequest request) {
        validateRequest(request);
        String username = normalizeUsername(request.getUsername());
        List<StoredUser> matchedUsers = jdbcTemplate.query(
                "select username, salt, password_hash from users where username = ?",
                (rs, rowNum) -> new StoredUser(
                        rs.getString("username"),
                        rs.getString("salt"),
                        rs.getString("password_hash")
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

    record StoredUser(String username, String salt, String passwordHash) {
    }
}
