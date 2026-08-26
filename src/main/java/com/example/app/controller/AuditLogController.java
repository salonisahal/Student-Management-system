package com.example.app.controller;

import com.example.app.dto.ApiResponse;
import com.example.app.dto.AuditLogDto;
import com.example.app.dto.PageResponse;
import com.example.app.service.AuditLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/audit-logs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Audit Logs", description = "ADMIN-only system audit trail")
public class AuditLogController {

    private final AuditLogService auditLogService;

    @Operation(summary = "Get paginated audit logs with optional filters")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AuditLogDto>>> getAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateTo) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        var result = PageResponse.from(auditLogService.getAuditLogs(userId, action, entityType, dateFrom, dateTo, pageable));
        return ResponseEntity.ok(ApiResponse.success("Audit logs retrieved successfully", result));
    }

    @Operation(summary = "Get a specific audit log entry by id")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AuditLogDto>> getAuditLog(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Audit log retrieved successfully", auditLogService.getAuditLog(id)));
    }
}
