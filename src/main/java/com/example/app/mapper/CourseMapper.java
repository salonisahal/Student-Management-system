package com.example.app.mapper;

import com.example.app.dto.CourseDto;
import com.example.app.entity.Course;

import java.util.HashSet;

public final class CourseMapper {
    private CourseMapper() {
    }

    public static CourseDto toDto(Course c) {
        return toDto(c, 0L);
    }

    public static CourseDto toDto(Course c, long enrolledCount) {
        if (c == null) return null;
        CourseDto dto = new CourseDto();
        dto.setId(c.getId());
        dto.setCourseCode(c.getCourseCode());
        dto.setCourseName(c.getCourseName());
        dto.setDescription(c.getDescription());
        dto.setCredits(c.getCredits());
        dto.setDepartment(c.getDepartment());
        dto.setTeacherId(c.getTeacherId());
        dto.setMaxCapacity(c.getMaxCapacity());
        dto.setEnrolledCount(enrolledCount);
        dto.setSeatsAvailable(c.getMaxCapacity() == null ? null : (int) Math.max(0, c.getMaxCapacity() - enrolledCount));
        dto.setPrerequisiteCourseIds(c.getPrerequisiteCourseIds() == null ? new HashSet<>() : new HashSet<>(c.getPrerequisiteCourseIds()));
        dto.setStatus(c.getStatus());
        dto.setCreatedAt(c.getCreatedAt());
        dto.setUpdatedAt(c.getUpdatedAt());
        return dto;
    }
}
