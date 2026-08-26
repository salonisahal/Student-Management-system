package com.example.app.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

@Data
public class StudentCreateRequest {
    @NotBlank
    private String studentNumber;

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

    @Past
    private LocalDate dateOfBirth;

    private String gender;

    private String address;

    private String department;

    @PastOrPresent
    private LocalDate admissionDate;
}
