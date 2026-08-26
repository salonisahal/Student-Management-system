package com.example.app.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GradeSummaryDto {

    private long totalCourses;

    @Schema(description = "Sum of credit hours across all graded courses")
    private int totalCredits;

    @Schema(description = "Simple (unweighted) average of marks across all graded courses")
    private double averageMarks;

    @Schema(description = "Average marks weighted by each course's credit hours")
    private double weightedAverageMarks;

    private double highestMarks;
    private double lowestMarks;

    @Schema(description = "Credit-weighted GPA on a 4.0 scale, computed from each course's letter grade and credit hours")
    private double gpa;

    @Schema(description = "Overall letter grade derived from the credit-weighted average marks")
    private String overallGrade;
}
