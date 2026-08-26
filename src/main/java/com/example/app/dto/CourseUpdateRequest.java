package com.example.app.dto;

import com.example.app.entity.UserStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Set;

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

    /**
     * IDs of courses a student must have already passed before they can enroll in this course.
     * Null means "leave unchanged"; an empty set explicitly clears all prerequisites.
     */
    private Set<Long> prerequisiteCourseIds;
}
