package com.example.app.service;

import com.example.app.entity.RefreshToken;
import com.example.app.exception.ExpiredTokenException;
import com.example.app.exception.InvalidTokenException;
import com.example.app.repository.RefreshTokenRepository;
import com.example.app.util.TokenHashUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${app.jwt.refresh-expiration-ms:604800000}")
    private long refreshExpirationMs;

    @Transactional
    public String createRefreshToken(Long userId) {
        String rawToken = TokenHashUtil.generateRawToken();
        RefreshToken token = new RefreshToken();
        token.setUserId(userId);
        token.setTokenHash(TokenHashUtil.hash(rawToken));
        token.setExpiryDate(LocalDateTime.now().plusSeconds(refreshExpirationMs / 1000));
        token.setRevoked(false);
        refreshTokenRepository.save(token);
        return rawToken;
    }

    @Transactional(readOnly = true)
    public RefreshToken validateAndGet(String rawToken) {
        String hash = TokenHashUtil.hash(rawToken);
        RefreshToken token = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new InvalidTokenException("Invalid refresh token"));
        if (token.isRevoked()) {
            throw new InvalidTokenException("Refresh token has been revoked");
        }
        if (token.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new ExpiredTokenException("Refresh token has expired");
        }
        return token;
    }

    @Transactional
    public void revoke(String rawToken) {
        String hash = TokenHashUtil.hash(rawToken);
        refreshTokenRepository.findByTokenHash(hash).ifPresent(token -> {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
        });
    }

    @Transactional
    public void revokeById(Long tokenId) {
        refreshTokenRepository.findById(tokenId).ifPresent(token -> {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
        });
    }

    @Transactional
    public void revokeAllForUser(Long userId) {
        refreshTokenRepository.revokeAllByUserId(userId);
    }
}
