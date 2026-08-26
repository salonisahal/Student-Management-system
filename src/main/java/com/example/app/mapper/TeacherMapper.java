package com.example.app.mapper;

import com.example.app.dto.TeacherDto;
import com.example.app.entity.Teacher;

public final class TeacherMapper {
    private TeacherMapper() {
    }

    public static TeacherDto toDto(Teacher t) {
        if (t == null) return null;
        TeacherDto dto = new TeacherDto();
        dto.setId(t.getId());
        dto.setUserId(t.getUserId());
        dto.setEmployeeNumber(t.getEmployeeNumber());
        dto.setFirstName(t.getFirstName());
        dto.setLastName(t.getLastName());
        dto.setEmail(t.getEmail());
        dto.setPhone(t.getPhone());
        dto.setDepartment(t.getDepartment());
        dto.setJoiningDate(t.getJoiningDate());
        dto.setStatus(t.getStatus());
        dto.setCreatedAt(t.getCreatedAt());
        dto.setUpdatedAt(t.getUpdatedAt());
        return dto;
    }
}
