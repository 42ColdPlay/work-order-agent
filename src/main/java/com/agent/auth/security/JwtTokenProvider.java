package com.agent.auth.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

    private final SecretKey secretKey;
    private final long expirationMs;

    public JwtTokenProvider(
            @Value("${jwt.secret:}") String secret,
            @Value("${jwt.expiration-ms:86400000}") long expirationMs,
            @Value("${spring.profiles.active:}") String activeProfiles) {
        this.secretKey = resolveSecretKey(secret, activeProfiles);
        this.expirationMs = expirationMs;
    }

    /**
     * 解析签名密钥。
     * <p>
     * 未配置 jwt.secret 时：
     * <ul>
     *   <li>生产环境（profile 含 prod）启动失败，避免弱密钥上线</li>
     *   <li>开发环境随机生成，本地零配置可启动（重启后旧 Token 失效）</li>
     * </ul>
     */
    private static SecretKey resolveSecretKey(String secret, String activeProfiles) {
        if (secret != null && !secret.isBlank()) {
            return Keys.hmacShaKeyFor(Base64.getDecoder().decode(secret));
        }
        if (activeProfiles != null && activeProfiles.contains("prod")) {
            throw new IllegalStateException("生产环境必须配置 jwt.secret（环境变量 JWT_SECRET）");
        }
        byte[] random = new byte[64];
        new SecureRandom().nextBytes(random);
        log.warn("未配置 jwt.secret，已生成随机密钥；重启后所有 Token 失效（仅限开发环境）");
        return Keys.hmacShaKeyFor(random);
    }

    public String generateToken(Long userId, String username) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(userId.toString())
                .claim("username", username)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey)
                .compact();
    }

    public Long getUserIdFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return Long.parseLong(claims.getSubject());
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public long getExpirationMs(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        long now = System.currentTimeMillis();
        long exp = claims.getExpiration().getTime();
        return Math.max(0, exp - now);
    }
}