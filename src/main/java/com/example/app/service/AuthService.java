package com.example.app.service;

import com.example.app.audit.AuditService;
import com.example.app.dto.*;
import com.example.app.entity.RefreshToken;
import com.example.app.entity.User;
import com.example.app.entity.UserStatus;
import com.example.app.exception.BadRequestException;
import com.example.app.exception.DuplicateResourceException;
import com.example.app.exception.UnauthorizedException;
import com.example.app.mapper.UserMapper;
import com.example.app.repository.UserRepository;
import com.example.app.security.JwtUtil;
import com.example.app.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;
    private final AuditService auditService;

    @Transactional
    public UserDto register(RegisterRequest request, String ipAddress) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("A user with this email already exists");
        }
        User user = new User();
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setPhone(request.getPhone());
        user.setRole(request.getRole());
        user.setStatus(UserStatus.ACTIVE);
        User saved = userRepository.save(user);
        auditService.record(saved.getId(), "USER_REGISTER", "User", String.valueOf(saved.getId()),
                "User registered with role " + saved.getRole(), ipAddress);
        return UserMapper.toDto(saved);
    }

    @Transactional
    public JwtResponse login(LoginRequest request, String ipAddress) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            auditService.record(user.getId(), "LOGIN_FAILED", "User", String.valueOf(user.getId()),
                    "Invalid password attempt", ipAddress);
            throw new UnauthorizedException("Invalid email or password");
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("Account is not active: " + user.getStatus());
        }

        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getEmail(), user.getRole().name());
        String refreshToken = refreshTokenService.createRefreshToken(user.getId());

        auditService.record(user.getId(), "LOGIN", "User", String.valueOf(user.getId()), "User logged in", ipAddress);

        return new JwtResponse(accessToken, refreshToken, jwtUtil.getAccessExpirationSeconds(), UserMapper.toDto(user));
    }

    @Transactional
    public JwtResponse refresh(RefreshTokenRequest request, String ipAddress) {
        RefreshToken existing = refreshTokenService.validateAndGet(request.getRefreshToken());
        User user = userRepository.findById(existing.getUserId())
                .orElseThrow(() -> new UnauthorizedException("User associated with token no longer exists"));

        // rotate refresh token
        refreshTokenService.revokeById(existing.getId());
        String newRefreshToken = refreshTokenService.createRefreshToken(user.getId());
        String newAccessToken = jwtUtil.generateAccessToken(user.getId(), user.getEmail(), user.getRole().name());

        auditService.record(user.getId(), "TOKEN_REFRESH", "User", String.valueOf(user.getId()), "Access token refreshed", ipAddress);

        return new JwtResponse(newAccessToken, newRefreshToken, jwtUtil.getAccessExpirationSeconds(), UserMapper.toDto(user));
    }

    @Transactional
    public void logout(RefreshTokenRequest request, String ipAddress) {
        refreshTokenService.revoke(request.getRefreshToken());
        Long userId = SecurityUtil.currentUserId();
        auditService.record(userId, "LOGOUT", "User", String.valueOf(userId), "User logged out", ipAddress);
    }

    @Transactional
    public void changePassword(ChangePasswordRequest request, String ipAddress) {
        Long userId = SecurityUtil.currentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BadRequestException("Current password is incorrect");
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        refreshTokenService.revokeAllForUser(userId);

        auditService.record(userId, "CHANGE_PASSWORD", "User", String.valueOf(userId), "Password changed", ipAddress);
    }
}
