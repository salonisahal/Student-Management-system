package com.example.app.controller;

import com.example.app.dto.*;
import com.example.app.service.EnrollmentService;
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
@RequestMapping("/api/v1/enrollments")
@RequiredArgsConstructor
@Tag(name = "Enrollments", description = "Student-course enrollment management")
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    @Operation(summary = "Enroll a student in a course (ADMIN only)")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<EnrollmentDto>> createEnrollment(@Valid @RequestBody EnrollmentCreateRequest request, HttpServletRequest httpRequest) {
        EnrollmentDto enrollment = enrollmentService.createEnrollment(request, SecurityUtil.currentUserId(), httpRequest.getRemoteAddr());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created("Enrollment created successfully", enrollment));
    }

    @Operation(summary = "Get paginated enrollments (ADMIN sees all, TEACHER sees assigned courses)")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public ResponseEntity<ApiResponse<PageResponse<EnrollmentDto>>> getEnrollments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false) Long courseId) {
        Pageable pageable = PageRequest.of(page, size);
        var result = PageResponse.from(enrollmentService.getEnrollments(studentId, courseId, pageable, SecurityUtil.currentUser()));
        return ResponseEntity.ok(ApiResponse.success("Enrollments retrieved successfully", result));
    }

    @Operation(summary = "Get enrollment details by id")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER','STUDENT')")
    public ResponseEntity<ApiResponse<EnrollmentDto>> getEnrollment(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Enrollment retrieved successfully",
                enrollmentService.getEnrollment(id, SecurityUtil.currentUser())));
    }

    @Operation(summary = "Remove/cancel an enrollment (ADMIN only)")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteEnrollment(@PathVariable Long id, HttpServletRequest httpRequest) {
        enrollmentService.deleteEnrollment(id, SecurityUtil.currentUserId(), httpRequest.getRemoteAddr());
        return ResponseEntity.noContent().build();
    }
}
