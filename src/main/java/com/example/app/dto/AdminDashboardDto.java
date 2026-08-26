package com.example.app.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminDashboardDto {
    private long totalStudents;
    private long totalTeachers;
    private long totalCourses;
    private long activeUsers;
    private long inactiveUsers;
    private Map<String, Long> enrollmentStatistics;
    private Map<String, Long> attendanceStatistics;
    private Map<String, Long> gradeStatistics;
}
