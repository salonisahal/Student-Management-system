package com.example.app.controller;

import com.example.app.dto.*;
import com.example.app.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Registration, login, token refresh, logout, password change")
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Register a new user (ADMIN only)")
    @PostMapping("/register")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserDto>> register(@Valid @RequestBody RegisterRequest request, HttpServletRequest httpRequest) {
        UserDto user = authService.register(request, httpRequest.getRemoteAddr());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created("User registered successfully", user));
    }

    @Operation(summary = "Login with email and password")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<JwtResponse>> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        JwtResponse response = authService.login(request, httpRequest.getRemoteAddr());
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    @Operation(summary = "Refresh access token using a valid refresh token")
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<JwtResponse>> refresh(@Valid @RequestBody RefreshTokenRequest request, HttpServletRequest httpRequest) {
        JwtResponse response = authService.refresh(request, httpRequest.getRemoteAddr());
        return ResponseEntity.ok(ApiResponse.success("Token refreshed successfully", response));
    }

    @Operation(summary = "Logout - revoke the given refresh token")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Object>> logout(@Valid @RequestBody RefreshTokenRequest request, HttpServletRequest httpRequest) {
        authService.logout(request, httpRequest.getRemoteAddr());
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully", null));
    }

    @Operation(summary = "Change the authenticated user's password")
    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Object>> changePassword(@Valid @RequestBody ChangePasswordRequest request, HttpServletRequest httpRequest) {
        authService.changePassword(request, httpRequest.getRemoteAddr());
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully", null));
    }
}
