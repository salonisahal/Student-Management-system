package com.example.app.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GradeDto {
    private Long id;
    private Long studentId;
    private Long courseId;
    private double marks;
    private String grade;
    private String remarks;
    private Long gradedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
