package com.example.app.dto;

import com.example.app.entity.UserStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;

@Data
public class StudentUpdateRequest {
    @NotBlank
    private String firstName;

    @NotBlank
    private String lastName;

    @NotBlank
    @Email
    private String email;

    private String phone;
    private LocalDate dateOfBirth;
    private String gender;
    private String address;

    // Administrative fields - only honored when caller is ADMIN (enforced in service layer)
    private String department;
    private String studentNumber;
    private UserStatus status;
}
