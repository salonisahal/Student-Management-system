package com.example.app.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Set;

@Data
public class CourseCreateRequest {
    @NotBlank
    private String courseCode;

    @NotBlank
    private String courseName;

    private String description;

    @Min(0)
    private int credits;

    private String department;

    private Long teacherId;

    @Min(value = 1, message = "maxCapacity must be at least 1")
    private Integer maxCapacity;

    /**
     * IDs of courses a student must have already passed before they can enroll in this course.
     */
    private Set<Long> prerequisiteCourseIds;
}
