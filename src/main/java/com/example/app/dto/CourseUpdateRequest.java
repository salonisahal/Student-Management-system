package com.example.app.dto;

import com.example.app.entity.UserStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CourseUpdateRequest {
    @NotBlank
    private String courseName;

    private String description;

    @Min(0)
    private int credits;

    private String department;

    private UserStatus status;

    @Min(value = 1, message = "maxCapacity must be at least 1")
    private Integer maxCapacity;
}
