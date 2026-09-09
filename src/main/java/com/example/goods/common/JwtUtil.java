package com.example.goods.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtil {

    @Value("${jwt.secret:goods_secret_key_2024_secure_token}")
    private String secret;

    @Value("${jwt.expiration:86400000}")
    private long expiration;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public String generateToken(String username, Long userId, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("username", username);  // 添加 username 到 claims
        claims.put("userId", userId);
        claims.put("role", role);
        claims.put("iat", new Date().getTime());
        claims.put("exp", new Date().getTime() + expiration);

        try {
            String header = base64UrlEncode("{\"alg\":\"HS256\",\"typ\":\"JWT\"}");
            String payload = base64UrlEncode(objectMapper.writeValueAsString(claims));
            String signature = hmacSha256(header + "." + payload, secret);

            return header + "." + payload + "." + signature;
        } catch (Exception e) {
            throw new RuntimeException("生成 Token 失败", e);
        }
    }

    public Boolean validateToken(String token, String username) {
        final String extractedUsername = extractUsername(token);
        return (extractedUsername.equals(username) && !isTokenExpired(token));
    }

    public String extractUsername(String token) {
        Map<String, Object> claims = extractAllClaims(token);
        // username 存储在 sub 字段中，这里我们从自定义字段获取
        return (String) claims.get("username");
    }

    public Long extractUserId(String token) {
        Map<String, Object> claims = extractAllClaims(token);
        Number userId = (Number) claims.get("userId");
        return userId != null ? userId.longValue() : null;
    }

    public String extractRole(String token) {
        Map<String, Object> claims = extractAllClaims(token);
        return (String) claims.get("role");
    }

    public Date extractExpiration(String token) {
        Map<String, Object> claims = extractAllClaims(token);
        Number exp = (Number) claims.get("exp");
        return exp != null ? new Date(exp.longValue()) : null;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractAllClaims(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                throw new RuntimeException("无效的 Token 格式");
            }

            String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
            return objectMapper.readValue(payload, Map.class);
        } catch (Exception e) {
            throw new RuntimeException("解析 Token 失败", e);
        }
    }

    private Boolean isTokenExpired(String token) {
        Date expiration = extractExpiration(token);
        return expiration != null && expiration.before(new Date());
    }

    private String base64UrlEncode(String data) {
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(data.getBytes(StandardCharsets.UTF_8));
    }

    private String hmacSha256(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("HMAC-SHA256 签名失败", e);
        }
    }
}