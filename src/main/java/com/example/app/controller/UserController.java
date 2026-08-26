package com.example.app.controller;

import com.example.app.dto.*;
import com.example.app.entity.Role;
import com.example.app.entity.UserStatus;
import com.example.app.service.UserService;
import com.example.app.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "User Management", description = "ADMIN-only user account management")
public class UserController {

    private final UserService userService;

    @Operation(summary = "Create a new user")
    @PostMapping
    public ResponseEntity<ApiResponse<UserDto>> createUser(@Valid @RequestBody UserCreateRequest request, HttpServletRequest httpRequest) {
        UserDto user = userService.createUser(request, SecurityUtil.currentUserId(), httpRequest.getRemoteAddr());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created("User created successfully", user));
    }

    @Operation(summary = "Get paginated users with optional role/status/search filters")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<UserDto>>> getUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sort,
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(required = false) String search) {
        Pageable pageable = PageRequest.of(page, size, org.springframework.data.domain.Sort.by(sort));
        var result = PageResponse.from(userService.getUsers(role, status, search, pageable));
        return ResponseEntity.ok(ApiResponse.success("Users retrieved successfully", result));
    }

    @Operation(summary = "Get a specific user by id")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserDto>> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("User retrieved successfully", userService.getUser(id)));
    }

    @Operation(summary = "Update a user's profile information")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserDto>> updateUser(@PathVariable Long id, @Valid @RequestBody UserUpdateRequest request,
                                                            HttpServletRequest httpRequest) {
        UserDto user = userService.updateUser(id, request, SecurityUtil.currentUserId(), httpRequest.getRemoteAddr());
        return ResponseEntity.ok(ApiResponse.success("User updated successfully", user));
    }

    @Operation(summary = "Activate, deactivate, or lock a user")
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<UserDto>> updateStatus(@PathVariable Long id, @Valid @RequestBody UserStatusUpdateRequest request,
                                                              HttpServletRequest httpRequest) {
        UserDto user = userService.updateStatus(id, request, SecurityUtil.currentUserId(), httpRequest.getRemoteAddr());
        return ResponseEntity.ok(ApiResponse.success("User status updated successfully", user));
    }

    @Operation(summary = "Delete (deactivate) a user")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id, HttpServletRequest httpRequest) {
        userService.deleteUser(id, SecurityUtil.currentUserId(), httpRequest.getRemoteAddr());
        return ResponseEntity.noContent().build();
    }
}
