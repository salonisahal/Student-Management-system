package com.example.app.mapper;

import com.example.app.dto.CourseDto;
import com.example.app.entity.Course;

public final class CourseMapper {
    private CourseMapper() {
    }

    public static CourseDto toDto(Course c) {
        if (c == null) return null;
        CourseDto dto = new CourseDto();
        dto.setId(c.getId());
        dto.setCourseCode(c.getCourseCode());
        dto.setCourseName(c.getCourseName());
        dto.setDescription(c.getDescription());
        dto.setCredits(c.getCredits());
        dto.setDepartment(c.getDepartment());
        dto.setTeacherId(c.getTeacherId());
        dto.setStatus(c.getStatus());
        dto.setCreatedAt(c.getCreatedAt());
        dto.setUpdatedAt(c.getUpdatedAt());
        return dto;
    }
}
