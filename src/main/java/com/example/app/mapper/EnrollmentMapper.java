package com.example.app.mapper;

import com.example.app.dto.EnrollmentDto;
import com.example.app.entity.Enrollment;
import com.example.app.entity.EnrollmentStatus;

public final class EnrollmentMapper {
    private EnrollmentMapper() {
    }

    public static EnrollmentDto toDto(Enrollment e) {
        return toDto(e, null);
    }

    public static EnrollmentDto toDto(Enrollment e, Integer waitlistPosition) {
        if (e == null) return null;
        EnrollmentDto dto = new EnrollmentDto();
        dto.setId(e.getId());
        dto.setStudentId(e.getStudentId());
        dto.setCourseId(e.getCourseId());
        dto.setEnrollmentDate(e.getEnrollmentDate());
        dto.setStatus(e.getStatus());
        dto.setWaitlistPosition(e.getStatus() == EnrollmentStatus.WAITLISTED ? waitlistPosition : null);
        dto.setCreatedAt(e.getCreatedAt());
        dto.setUpdatedAt(e.getUpdatedAt());
        return dto;
    }
}
