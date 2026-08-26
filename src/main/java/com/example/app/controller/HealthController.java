package com.example.app.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/health")
@RequiredArgsConstructor
@Tag(name = "Health", description = "Public application health check")
public class HealthController {

    private final DataSource dataSource;

    @Operation(summary = "Get application and database health status (public)")
    @GetMapping
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "UP");
        body.put("timestamp", LocalDateTime.now());

        String dbStatus;
        try (Connection connection = dataSource.getConnection()) {
            dbStatus = connection.isValid(2) ? "UP" : "DOWN";
        } catch (Exception e) {
            dbStatus = "DOWN";
        }
        body.put("database", dbStatus);
        return ResponseEntity.ok(body);
    }
}
