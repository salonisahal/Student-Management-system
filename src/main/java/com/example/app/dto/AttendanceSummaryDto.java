package com.example.app.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceSummaryDto {
    private long totalClasses;
    private long present;
    private long absent;
    private long late;
    private long excused;
    private double attendancePercentage;
}
