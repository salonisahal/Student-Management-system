package com.example.app.controller;

import com.example.app.dto.*;
import com.example.app.entity.UserStatus;
import com.example.app.security.UserPrincipal;
import com.example.app.service.StudentService;
import com.example.app.util.SecurityUtil;
import com.example.app.util.SortUtil;
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
@RequestMapping("/api/v1/students")
@RequiredArgsConstructor
@Tag(name = "Students", description = "Student management endpoints")
public class StudentController {

    private final StudentService studentService;

    @Operation(summary = "Create a student (ADMIN only)")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<StudentDto>> createStudent(@Valid @RequestBody StudentCreateRequest request, HttpServletRequest httpRequest) {
        StudentDto student = studentService.createStudent(request, SecurityUtil.currentUserId(), httpRequest.getRemoteAddr());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created("Student created successfully", student));
    }

    @Operation(summary = "Get paginated students (ADMIN sees all, TEACHER sees only assigned students)")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public ResponseEntity<ApiResponse<PageResponse<StudentDto>>> getStudents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sort,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(required = false) String search) {
        Pageable pageable = PageRequest.of(page, size, SortUtil.parse(sort, com.example.app.entity.Student.class));
        UserPrincipal principal = SecurityUtil.currentUser();
        var result = PageResponse.from(studentService.getStudents(department, status, search, pageable, principal));
        return ResponseEntity.ok(ApiResponse.success("Students retrieved successfully", result));
    }

    @Operation(summary = "Get student details by id")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER','STUDENT')")
    public ResponseEntity<ApiResponse<StudentDto>> getStudent(@PathVariable Long id) {
        StudentDto student = studentService.getStudent(id, SecurityUtil.currentUser());
        return ResponseEntity.ok(ApiResponse.success("Student retrieved successfully", student));
    }

    @Operation(summary = "Update a student (ADMIN or the student themselves)")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','STUDENT')")
    public ResponseEntity<ApiResponse<StudentDto>> updateStudent(@PathVariable Long id, @Valid @RequestBody StudentUpdateRequest request,
                                                                  HttpServletRequest httpRequest) {
        StudentDto student = studentService.updateStudent(id, request, SecurityUtil.currentUser(), httpRequest.getRemoteAddr());
        return ResponseEntity.ok(ApiResponse.success("Student updated successfully", student));
    }

    @Operation(summary = "Delete (deactivate) a student (ADMIN only)")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteStudent(@PathVariable Long id, HttpServletRequest httpRequest) {
        studentService.deleteStudent(id, SecurityUtil.currentUserId(), httpRequest.getRemoteAddr());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get courses enrolled by a student")
    @GetMapping("/{id}/courses")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER','STUDENT')")
    public ResponseEntity<ApiResponse<List<CourseDto>>> getStudentCourses(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Student courses retrieved successfully",
                studentService.getStudentCourses(id, SecurityUtil.currentUser())));
    }

    @Operation(summary = "Get a student's attendance records")
    @GetMapping("/{id}/attendance")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER','STUDENT')")
    public ResponseEntity<ApiResponse<List<AttendanceDto>>> getStudentAttendance(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Student attendance retrieved successfully",
                studentService.getStudentAttendance(id, SecurityUtil.currentUser())));
    }

    @Operation(summary = "Get a student's grades")
    @GetMapping("/{id}/grades")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER','STUDENT')")
    public ResponseEntity<ApiResponse<List<GradeDto>>> getStudentGrades(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Student grades retrieved successfully",
                studentService.getStudentGrades(id, SecurityUtil.currentUser())));
    }

    @Operation(summary = "Get a student's enrollments")
    @GetMapping("/{studentId}/enrollments")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER','STUDENT')")
    public ResponseEntity<ApiResponse<List<EnrollmentDto>>> getStudentEnrollments(@PathVariable Long studentId) {
        return ResponseEntity.ok(ApiResponse.success("Student enrollments retrieved successfully",
                studentService.getStudentEnrollments(studentId, SecurityUtil.currentUser())));
    }

    @Operation(summary = "Get a student's attendance summary (totals + percentage)")
    @GetMapping("/{studentId}/attendance/summary")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER','STUDENT')")
    public ResponseEntity<ApiResponse<AttendanceSummaryDto>> getAttendanceSummary(@PathVariable Long studentId) {
        return ResponseEntity.ok(ApiResponse.success("Attendance summary retrieved successfully",
                studentService.getAttendanceSummary(studentId, SecurityUtil.currentUser())));
    }

    @Operation(summary = "Get a student's academic/grade summary")
    @GetMapping("/{studentId}/grades/summary")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER','STUDENT')")
    public ResponseEntity<ApiResponse<GradeSummaryDto>> getGradeSummary(@PathVariable Long studentId) {
        return ResponseEntity.ok(ApiResponse.success("Grade summary retrieved successfully",
                studentService.getGradeSummary(studentId, SecurityUtil.currentUser())));
    }
}
