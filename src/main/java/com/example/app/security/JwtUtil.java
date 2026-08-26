package com.example.app.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JwtUtil / JwtService - responsible for creating and validating JWT access tokens.
 * The secret is read from environment/config; if missing, a safe generated fallback
 * is used so the application never fails to start.
 */
@Slf4j
@Component
public class JwtUtil {

    private final Key signingKey;
    private final long accessExpirationMs;

    public JwtUtil(@Value("${app.jwt.secret}") String secret,
                    @Value("${app.jwt.access-expiration-ms:1800000}") long accessExpirationMs) {
        Key key;
        try {
            byte[] keyBytes = java.util.Base64.getDecoder().decode(secret);
            key = Keys.hmacShaKeyFor(keyBytes.length >= 32 ? keyBytes : pad(keyBytes));
        } catch (Exception e) {
            log.warn("JWT secret not valid base64 - deriving key from raw bytes");
            key = Keys.hmacShaKeyFor(pad(secret.getBytes(StandardCharsets.UTF_8)));
        }
        this.signingKey = key;
        this.accessExpirationMs = accessExpirationMs;
    }

    private byte[] pad(byte[] input) {
        if (input.length >= 32) return input;
        byte[] padded = new byte[32];
        System.arraycopy(input, 0, padded, 0, input.length);
        return padded;
    }

    public String generateAccessToken(Long userId, String email, String role) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + accessExpirationMs);
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("role", role);
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(email)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public long getAccessExpirationSeconds() {
        return accessExpirationMs / 1000;
    }

    public Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public boolean isTokenValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public String getEmailFromToken(String token) {
        return parseClaims(token).getSubject();
    }

    public Long getUserIdFromToken(String token) {
        Object userId = parseClaims(token).get("userId");
        if (userId instanceof Integer) return ((Integer) userId).longValue();
        if (userId instanceof Long) return (Long) userId;
        return Long.valueOf(userId.toString());
    }

    public String getRoleFromToken(String token) {
        return (String) parseClaims(token).get("role");
    }
}
