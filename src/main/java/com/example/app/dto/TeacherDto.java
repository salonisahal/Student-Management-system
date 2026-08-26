package com.example.app.dto;

import com.example.app.entity.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TeacherDto {
    private Long id;
    private Long userId;
    private String employeeNumber;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String department;
    private LocalDate joiningDate;
    private UserStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
