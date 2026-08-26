package com.example.app.controller;

import com.example.app.dto.AdminDashboardDto;
import com.example.app.dto.ApiResponse;
import com.example.app.dto.StudentDashboardDto;
import com.example.app.dto.TeacherDashboardDto;
import com.example.app.service.DashboardService;
import com.example.app.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Role-specific statistics dashboards")
public class DashboardController {

    private final DashboardService dashboardService;

    @Operation(summary = "Admin dashboard statistics (ADMIN only)")
    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AdminDashboardDto>> getAdminDashboard() {
        return ResponseEntity.ok(ApiResponse.success("Admin dashboard retrieved successfully", dashboardService.getAdminDashboard()));
    }

    @Operation(summary = "Teacher dashboard statistics for assigned courses (TEACHER only)")
    @GetMapping("/teacher")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<TeacherDashboardDto>> getTeacherDashboard() {
        return ResponseEntity.ok(ApiResponse.success("Teacher dashboard retrieved successfully",
                dashboardService.getTeacherDashboard(SecurityUtil.currentUserId())));
    }

    @Operation(summary = "Student dashboard statistics for own data (STUDENT only)")
    @GetMapping("/student")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<StudentDashboardDto>> getStudentDashboard() {
        return ResponseEntity.ok(ApiResponse.success("Student dashboard retrieved successfully",
                dashboardService.getStudentDashboard(SecurityUtil.currentUserId())));
    }
}
