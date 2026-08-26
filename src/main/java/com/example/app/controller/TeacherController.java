package com.example.app.controller;

import com.example.app.dto.*;
import com.example.app.service.TeacherService;
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

import java.util.List;

@RestController
@RequestMapping("/api/v1/teachers")
@RequiredArgsConstructor
@Tag(name = "Teachers", description = "Teacher management endpoints")
public class TeacherController {

    private final TeacherService teacherService;

    @Operation(summary = "Create a teacher (ADMIN only)")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<TeacherDto>> createTeacher(@Valid @RequestBody TeacherCreateRequest request, HttpServletRequest httpRequest) {
        TeacherDto teacher = teacherService.createTeacher(request, SecurityUtil.currentUserId(), httpRequest.getRemoteAddr());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created("Teacher created successfully", teacher));
    }

    @Operation(summary = "Get paginated teachers")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public ResponseEntity<ApiResponse<PageResponse<TeacherDto>>> getTeachers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sort,
            @RequestParam(required = false) String search) {
        Pageable pageable = PageRequest.of(page, size, org.springframework.data.domain.Sort.by(sort));
        var result = PageResponse.from(teacherService.getTeachers(search, pageable));
        return ResponseEntity.ok(ApiResponse.success("Teachers retrieved successfully", result));
    }

    @Operation(summary = "Get teacher details by id")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public ResponseEntity<ApiResponse<TeacherDto>> getTeacher(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Teacher retrieved successfully", teacherService.getTeacher(id)));
    }

    @Operation(summary = "Update a teacher (ADMIN or the teacher themselves)")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public ResponseEntity<ApiResponse<TeacherDto>> updateTeacher(@PathVariable Long id, @Valid @RequestBody TeacherUpdateRequest request,
                                                                  HttpServletRequest httpRequest) {
        TeacherDto teacher = teacherService.updateTeacher(id, request, SecurityUtil.currentUser(), httpRequest.getRemoteAddr());
        return ResponseEntity.ok(ApiResponse.success("Teacher updated successfully", teacher));
    }

    @Operation(summary = "Delete (deactivate) a teacher (ADMIN only)")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteTeacher(@PathVariable Long id, HttpServletRequest httpRequest) {
        teacherService.deleteTeacher(id, SecurityUtil.currentUserId(), httpRequest.getRemoteAddr());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get courses assigned to a teacher")
    @GetMapping("/{id}/courses")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public ResponseEntity<ApiResponse<List<CourseDto>>> getTeacherCourses(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Teacher courses retrieved successfully",
                teacherService.getTeacherCourses(id, SecurityUtil.currentUser())));
    }
}
