package com.example.app.mapper;

import com.example.app.dto.AttendanceDto;
import com.example.app.entity.Attendance;

public final class AttendanceMapper {
    private AttendanceMapper() {
    }

    public static AttendanceDto toDto(Attendance a) {
        if (a == null) return null;
        AttendanceDto dto = new AttendanceDto();
        dto.setId(a.getId());
        dto.setStudentId(a.getStudentId());
        dto.setCourseId(a.getCourseId());
        dto.setAttendanceDate(a.getAttendanceDate());
        dto.setStatus(a.getStatus());
        dto.setRemarks(a.getRemarks());
        dto.setMarkedBy(a.getMarkedBy());
        dto.setCreatedAt(a.getCreatedAt());
        dto.setUpdatedAt(a.getUpdatedAt());
        return dto;
    }
}
