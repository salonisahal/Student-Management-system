package com.example.app.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

@Data
public class TeacherCreateRequest {
    @NotBlank
    private String employeeNumber;

    @NotBlank
    private String firstName;

    @NotBlank
    private String lastName;

    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Size(min = 6, max = 100)
    private String password;

    private String phone;

    private String department;

    @PastOrPresent
    private LocalDate joiningDate;
}
