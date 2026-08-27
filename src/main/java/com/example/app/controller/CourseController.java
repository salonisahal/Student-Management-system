package com.example.app.controller;

import com.example.app.dto.*;
import com.example.app.entity.UserStatus;
import com.example.app.service.CourseService;
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
@RequestMapping("/api/v1/courses")
@RequiredArgsConstructor
@Tag(name = "Courses", description = "Course management endpoints")
public class CourseController {

    private final CourseService courseService;

    @Operation(summary = "Create a course (ADMIN only)",
            description = "Optionally accepts prerequisiteCourseIds - IDs of courses a student must have " +
                    "already passed before they can enroll in this course (e.g. 'Data Structures I' as a " +
                    "prerequisite of 'Data Structures II'). Returns 404 if any prerequisite course id does not exist.")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CourseDto>> createCourse(@Valid @RequestBody CourseCreateRequest request, HttpServletRequest httpRequest) {
        CourseDto course = courseService.createCourse(request, SecurityUtil.currentUserId(), httpRequest.getRemoteAddr());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created("Course created successfully", course));
    }

    @Operation(summary = "Get paginated courses with optional filters")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER','STUDENT')")
    public ResponseEntity<ApiResponse<PageResponse<CourseDto>>> getCourses(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sort,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(required = false) Long teacherId,
            @RequestParam(required = false) String search) {
        Pageable pageable = PageRequest.of(page, size, SortUtil.parse(sort, com.example.app.entity.Course.class));
        var result = PageResponse.from(courseService.getCourses(department, status, teacherId, search, pageable));
        return ResponseEntity.ok(ApiResponse.success("Courses retrieved successfully", result));
    }

    @Operation(summary = "Get course details by id")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER','STUDENT')")
    public ResponseEntity<ApiResponse<CourseDto>> getCourse(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Course retrieved successfully", courseService.getCourse(id)));
    }

    @Operation(summary = "Update a course (ADMIN only)",
            description = "If prerequisiteCourseIds is provided (even as an empty array) it replaces the " +
                    "course's current prerequisite list; omit the field to leave prerequisites unchanged. " +
                    "Returns 400 if a course is set as its own prerequisite or if the change would create a " +
                    "circular prerequisite dependency, and 404 if any prerequisite course id does not exist.")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CourseDto>> updateCourse(@PathVariable Long id, @Valid @RequestBody CourseUpdateRequest request,
                                                                HttpServletRequest httpRequest) {
        CourseDto course = courseService.updateCourse(id, request, SecurityUtil.currentUserId(), httpRequest.getRemoteAddr());
        return ResponseEntity.ok(ApiResponse.success("Course updated successfully", course));
    }

    @Operation(summary = "Delete (deactivate) a course (ADMIN only)")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteCourse(@PathVariable Long id, HttpServletRequest httpRequest) {
        courseService.deleteCourse(id, SecurityUtil.currentUserId(), httpRequest.getRemoteAddr());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Assign a teacher to a course (ADMIN only)")
    @PutMapping("/{courseId}/teacher/{teacherId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CourseDto>> assignTeacher(@PathVariable Long courseId, @PathVariable Long teacherId,
                                                                 HttpServletRequest httpRequest) {
        CourseDto course = courseService.assignTeacher(courseId, teacherId, SecurityUtil.currentUserId(), httpRequest.getRemoteAddr());
        return ResponseEntity.ok(ApiResponse.success("Teacher assigned successfully", course));
    }

    @Operation(summary = "Get students enrolled in a course (ADMIN or the assigned teacher)")
    @GetMapping("/{courseId}/students")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public ResponseEntity<ApiResponse<List<StudentDto>>> getCourseStudents(@PathVariable Long courseId) {
        return ResponseEntity.ok(ApiResponse.success("Course students retrieved successfully",
                courseService.getCourseStudents(courseId, SecurityUtil.currentUser())));
    }
}
