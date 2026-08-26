package com.example.app.controller;

import com.example.app.dto.*;
import com.example.app.entity.AttendanceStatus;
import com.example.app.service.AttendanceService;
import com.example.app.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/attendance")
@RequiredArgsConstructor
@Tag(name = "Attendance", description = "Student attendance tracking")
public class AttendanceController {

    private final AttendanceService attendanceService;

    @Operation(summary = "Create an attendance record (ADMIN or assigned TEACHER)")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public ResponseEntity<ApiResponse<AttendanceDto>> createAttendance(@Valid @RequestBody AttendanceCreateRequest request, HttpServletRequest httpRequest) {
        AttendanceDto attendance = attendanceService.createAttendance(request, SecurityUtil.currentUser(), httpRequest.getRemoteAddr());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created("Attendance recorded successfully", attendance));
    }

    @Operation(summary = "Get attendance records with optional filters")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public ResponseEntity<ApiResponse<PageResponse<AttendanceDto>>> getAttendance(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) AttendanceStatus status) {
        Pageable pageable = PageRequest.of(page, size);
        var result = PageResponse.from(attendanceService.getAttendanceRecords(studentId, courseId, date, dateFrom, dateTo, status,
                pageable, SecurityUtil.currentUser()));
        return ResponseEntity.ok(ApiResponse.success("Attendance retrieved successfully", result));
    }

    @Operation(summary = "Get an attendance record by id")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER','STUDENT')")
    public ResponseEntity<ApiResponse<AttendanceDto>> getAttendanceRecord(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Attendance record retrieved successfully",
                attendanceService.getAttendance(id, SecurityUtil.currentUser())));
    }

    @Operation(summary = "Update an attendance record (ADMIN or assigned TEACHER)")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public ResponseEntity<ApiResponse<AttendanceDto>> updateAttendance(@PathVariable Long id, @Valid @RequestBody AttendanceUpdateRequest request,
                                                                        HttpServletRequest httpRequest) {
        AttendanceDto attendance = attendanceService.updateAttendance(id, request, SecurityUtil.currentUser(), httpRequest.getRemoteAddr());
        return ResponseEntity.ok(ApiResponse.success("Attendance updated successfully", attendance));
    }

    @Operation(summary = "Delete an attendance record (ADMIN or assigned TEACHER)")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public ResponseEntity<Void> deleteAttendance(@PathVariable Long id, HttpServletRequest httpRequest) {
        attendanceService.deleteAttendance(id, SecurityUtil.currentUser(), httpRequest.getRemoteAddr());
        return ResponseEntity.noContent().build();
    }
}
