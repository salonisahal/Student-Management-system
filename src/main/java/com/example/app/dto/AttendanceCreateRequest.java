package com.example.app.dto;

import com.example.app.entity.AttendanceStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import lombok.Data;

import java.time.LocalDate;

@Data
public class AttendanceCreateRequest {
    @NotNull
    private Long studentId;

    @NotNull
    private Long courseId;

    @NotNull
    @PastOrPresent
    private LocalDate attendanceDate;

    @NotNull
    private AttendanceStatus status;

    private String remarks;
}
