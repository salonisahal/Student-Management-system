package com.example.app.dto;

import com.example.app.entity.UserStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UserStatusUpdateRequest {
    @NotNull
    private UserStatus status;
}
