package com.example.app.controller;

import com.example.app.dto.ApiResponse;
import com.example.app.dto.ProfileUpdateRequest;
import com.example.app.dto.UserDto;
import com.example.app.service.ProfileService;
import com.example.app.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/profile")
@RequiredArgsConstructor
@Tag(name = "Profile", description = "Authenticated user's own profile")
public class ProfileController {

    private final ProfileService profileService;

    @Operation(summary = "Get the currently authenticated user's profile")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER','STUDENT')")
    public ResponseEntity<ApiResponse<UserDto>> getProfile() {
        return ResponseEntity.ok(ApiResponse.success("Profile retrieved successfully", profileService.getProfile(SecurityUtil.currentUserId())));
    }

    @Operation(summary = "Update the currently authenticated user's profile (role cannot be changed)")
    @PutMapping
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER','STUDENT')")
    public ResponseEntity<ApiResponse<UserDto>> updateProfile(@Valid @RequestBody ProfileUpdateRequest request, HttpServletRequest httpRequest) {
        UserDto profile = profileService.updateProfile(SecurityUtil.currentUserId(), request, httpRequest.getRemoteAddr());
        return ResponseEntity.ok(ApiResponse.success("Profile updated successfully", profile));
    }
}
