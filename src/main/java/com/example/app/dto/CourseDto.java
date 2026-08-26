package com.example.app.dto;

import com.example.app.entity.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CourseDto {
    private Long id;
    private String courseCode;
    private String courseName;
    private String description;
    private int credits;
    private String department;
    private Long teacherId;
    private Integer maxCapacity;
    private long enrolledCount;
    private Integer seatsAvailable;
    private Set<Long> prerequisiteCourseIds;
    private UserStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
