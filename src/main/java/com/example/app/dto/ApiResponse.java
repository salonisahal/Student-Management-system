package com.example.app.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
    private LocalDateTime timestamp;
    private int status;
    private String message;
    private T data;

    public static <T> ApiResponse<T> of(int status, String message, T data) {
        return new ApiResponse<>(LocalDateTime.now(), status, message, data);
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return of(200, message, data);
    }

    public static <T> ApiResponse<T> created(String message, T data) {
        return of(201, message, data);
    }
}
