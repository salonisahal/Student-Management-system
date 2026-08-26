package com.example.app.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentDashboardDto {
    private List<CourseDto> enrolledCourses;
    private double attendancePercentage;
    private List<GradeDto> grades;
    private GradeSummaryDto academicSummary;
}
