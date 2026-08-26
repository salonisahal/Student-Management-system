package com.example.app.dto;

import com.example.app.entity.AttendanceStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AttendanceUpdateRequest {
    @NotNull
    private AttendanceStatus status;

    private String remarks;
}
