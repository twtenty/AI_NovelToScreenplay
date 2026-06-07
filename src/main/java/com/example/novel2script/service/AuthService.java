package com.example.novel2script.service;

import com.example.novel2script.model.AuthRequest;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

@Service
public class AuthService {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_\\-]{3,20}$");
    private final Map<String, StoredUser> users = new ConcurrentHashMap<>();
    private final SecureRandom secureRandom = new SecureRandom();
    private final Path userStorePath;

    public AuthService(@Value("${USER_STORE_PATH:data/users.json}") String userStorePath) throws IOException {
        this.userStorePath = Path.of(userStorePath);
        loadUsers();
    }

    public synchronized String register(AuthRequest request) throws IOException {
        validateRequest(request);
        String username = normalizeUsername(request.getUsername());
        if (users.containsKey(username)) {
            throw new IllegalArgumentException("用户名已存在。");
        }

        String salt = generateSalt();
        users.put(username, new StoredUser(username, salt, hashPassword(request.getPassword(), salt)));
        saveUsers();
        return username;
    }

    public String login(AuthRequest request) {
        validateRequest(request);
        String username = normalizeUsername(request.getUsername());
        StoredUser user = users.get(username);
        if (user == null || !user.passwordHash.equals(hashPassword(request.getPassword(), user.salt))) {
            throw new IllegalArgumentException("用户名或密码错误。");
        }
        return username;
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

    private void loadUsers() throws IOException {
        if (!Files.exists(userStorePath)) {
            return;
        }
        Map<String, StoredUser> loadedUsers = OBJECT_MAPPER.readValue(
                Files.readString(userStorePath, StandardCharsets.UTF_8),
                new TypeReference<>() {
                }
        );
        users.putAll(loadedUsers);
    }

    private void saveUsers() throws IOException {
        Path parent = userStorePath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValue(userStorePath.toFile(), users);
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

    public static class StoredUser {
        public String username;
        public String salt;
        public String passwordHash;

        public StoredUser() {
        }

        public StoredUser(String username, String salt, String passwordHash) {
            this.username = username;
            this.salt = salt;
            this.passwordHash = passwordHash;
        }
    }
}
