package com.example.app.dto;

import com.example.app.entity.UserStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TeacherUpdateRequest {
    @NotBlank
    private String firstName;

    @NotBlank
    private String lastName;

    @NotBlank
    @Email
    private String email;

    private String phone;

    // Administrative fields - only honored when caller is ADMIN
    private String department;
    private UserStatus status;
}
