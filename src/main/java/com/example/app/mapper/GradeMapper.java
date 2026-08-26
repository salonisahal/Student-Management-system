package com.example.app.mapper;

import com.example.app.dto.GradeDto;
import com.example.app.entity.Grade;

public final class GradeMapper {
    private GradeMapper() {
    }

    public static GradeDto toDto(Grade g) {
        if (g == null) return null;
        GradeDto dto = new GradeDto();
        dto.setId(g.getId());
        dto.setStudentId(g.getStudentId());
        dto.setCourseId(g.getCourseId());
        dto.setMarks(g.getMarks());
        dto.setGrade(g.getGrade());
        dto.setRemarks(g.getRemarks());
        dto.setGradedBy(g.getGradedBy());
        dto.setCreatedAt(g.getCreatedAt());
        dto.setUpdatedAt(g.getUpdatedAt());
        return dto;
    }
}
