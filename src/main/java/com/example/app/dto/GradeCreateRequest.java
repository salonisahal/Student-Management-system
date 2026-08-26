package com.example.app.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class GradeCreateRequest {
    @NotNull
    private Long studentId;

    @NotNull
    private Long courseId;

    @NotNull
    @Min(0)
    @Max(100)
    private Double marks;

    private String remarks;
}
