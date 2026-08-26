package com.example.app.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class EnrollmentCreateRequest {
    @NotNull
    private Long studentId;

    @NotNull
    private Long courseId;
}
