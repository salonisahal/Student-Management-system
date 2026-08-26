package com.example.app.mapper;

import com.example.app.dto.StudentDto;
import com.example.app.entity.Student;

public final class StudentMapper {
    private StudentMapper() {
    }

    public static StudentDto toDto(Student s) {
        if (s == null) return null;
        StudentDto dto = new StudentDto();
        dto.setId(s.getId());
        dto.setUserId(s.getUserId());
        dto.setStudentNumber(s.getStudentNumber());
        dto.setFirstName(s.getFirstName());
        dto.setLastName(s.getLastName());
        dto.setEmail(s.getEmail());
        dto.setPhone(s.getPhone());
        dto.setDateOfBirth(s.getDateOfBirth());
        dto.setGender(s.getGender());
        dto.setAddress(s.getAddress());
        dto.setDepartment(s.getDepartment());
        dto.setAdmissionDate(s.getAdmissionDate());
        dto.setStatus(s.getStatus());
        dto.setCreatedAt(s.getCreatedAt());
        dto.setUpdatedAt(s.getUpdatedAt());
        return dto;
    }
}
