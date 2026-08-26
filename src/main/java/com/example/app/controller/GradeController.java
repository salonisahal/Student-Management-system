package com.example.app.controller;

import com.example.app.dto.*;
import com.example.app.service.GradeService;
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
@RequestMapping("/api/v1/grades")
@RequiredArgsConstructor
@Tag(name = "Grades", description = "Student grade management")
public class GradeController {

    private final GradeService gradeService;

    @Operation(summary = "Create a grade (ADMIN or assigned TEACHER) - grade letter is auto-calculated. " +
            "If the student's attendance in the course is below the minimum required percentage, the grade " +
            "is recorded as 'NE' (Not Eligible) instead of a marks-derived letter grade.")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public ResponseEntity<ApiResponse<GradeDto>> createGrade(@Valid @RequestBody GradeCreateRequest request, HttpServletRequest httpRequest) {
        GradeDto grade = gradeService.createGrade(request, SecurityUtil.currentUser(), httpRequest.getRemoteAddr());
        String message = "NE".equals(grade.getGrade())
                ? "Grade recorded as Not Eligible (NE) - student's attendance in this course is below the required minimum"
                : "Grade created successfully";
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(message, grade));
    }

    @Operation(summary = "Get grades with optional filters")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public ResponseEntity<ApiResponse<PageResponse<GradeDto>>> getGrades(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) String grade,
            @RequestParam(required = false) Double minMarks,
            @RequestParam(required = false) Double maxMarks) {
        Pageable pageable = PageRequest.of(page, size);
        var result = PageResponse.from(gradeService.getGrades(studentId, courseId, grade, minMarks, maxMarks, pageable, SecurityUtil.currentUser()));
        return ResponseEntity.ok(ApiResponse.success("Grades retrieved successfully", result));
    }

    @Operation(summary = "Get grade details by id")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER','STUDENT')")
    public ResponseEntity<ApiResponse<GradeDto>> getGrade(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Grade retrieved successfully",
                gradeService.getGrade(id, SecurityUtil.currentUser())));
    }

    @Operation(summary = "Update a grade (ADMIN or assigned TEACHER)")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public ResponseEntity<ApiResponse<GradeDto>> updateGrade(@PathVariable Long id, @Valid @RequestBody GradeUpdateRequest request,
                                                              HttpServletRequest httpRequest) {
        GradeDto grade = gradeService.updateGrade(id, request, SecurityUtil.currentUser(), httpRequest.getRemoteAddr());
        String message = "NE".equals(grade.getGrade())
                ? "Grade recorded as Not Eligible (NE) - student's attendance in this course is below the required minimum"
                : "Grade updated successfully";
        return ResponseEntity.ok(ApiResponse.success(message, grade));
    }

    @Operation(summary = "Delete a grade (ADMIN only)")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteGrade(@PathVariable Long id, HttpServletRequest httpRequest) {
        gradeService.deleteGrade(id, SecurityUtil.currentUserId(), httpRequest.getRemoteAddr());
        return ResponseEntity.noContent().build();
    }
}
