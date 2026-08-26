package com.example.app.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TeacherDashboardDto {
    private long assignedCourses;
    private long totalStudents;
    private Map<String, Long> attendanceStatistics;
    private Map<String, Long> gradeStatistics;
}
