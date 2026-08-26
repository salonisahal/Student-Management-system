package com.example.app.service;

import com.example.app.audit.AuditService;
import com.example.app.dto.LoginRequest;
import com.example.app.dto.RegisterRequest;
import com.example.app.entity.Role;
import com.example.app.entity.User;
import com.example.app.entity.UserStatus;
import com.example.app.exception.DuplicateResourceException;
import com.example.app.exception.UnauthorizedException;
import com.example.app.repository.UserRepository;
import com.example.app.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private RefreshTokenService refreshTokenService;
    @Mock
    private AuditService auditService;

    @InjectMocks
    private AuthService authService;

    private User activeUser;

    @BeforeEach
    void setUp() {
        activeUser = new User();
        activeUser.setId(1L);
        activeUser.setEmail("test@example.com");
        activeUser.setPassword("hashed");
        activeUser.setRole(Role.STUDENT);
        activeUser.setStatus(UserStatus.ACTIVE);
        activeUser.setCreatedAt(LocalDateTime.now());
        activeUser.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    void registerThrowsWhenEmailAlreadyExists() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@example.com");
        request.setFirstName("A");
        request.setLastName("B");
        request.setPassword("Password1");
        request.setRole(Role.STUDENT);

        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> authService.register(request, "127.0.0.1"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void loginThrowsUnauthorizedForWrongPassword() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("wrong");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        assertThrows(UnauthorizedException.class, () -> authService.login(request, "127.0.0.1"));
    }

    @Test
    void loginSucceedsAndReturnsTokens() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("correct");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("correct", "hashed")).thenReturn(true);
        when(jwtUtil.generateAccessToken(1L, "test@example.com", "STUDENT")).thenReturn("access-token");
        when(jwtUtil.getAccessExpirationSeconds()).thenReturn(1800L);
        when(refreshTokenService.createRefreshToken(1L)).thenReturn("refresh-token");

        var response = authService.login(request, "127.0.0.1");

        assertEquals("access-token", response.getAccessToken());
        assertEquals("refresh-token", response.getRefreshToken());
        assertEquals(1800L, response.getExpiresIn());
    }
}
