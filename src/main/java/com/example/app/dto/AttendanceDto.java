package com.example.app.dto;

import com.example.app.entity.AttendanceStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceDto {
    private Long id;
    private Long studentId;
    private Long courseId;
    private LocalDate attendanceDate;
    private AttendanceStatus status;
    private String remarks;
    private Long markedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
