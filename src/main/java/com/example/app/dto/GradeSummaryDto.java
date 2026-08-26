package com.example.app.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GradeSummaryDto {
    private long totalCourses;
    private double averageMarks;
    private double highestMarks;
    private double lowestMarks;
    private String overallGrade;
}
