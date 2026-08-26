package com.example.app.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ProfileUpdateRequest {
    @NotBlank
    private String firstName;

    @NotBlank
    private String lastName;

    private String phone;
}
