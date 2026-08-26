package com.example.app.dto;

import com.example.app.entity.EnrollmentStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EnrollmentDto {
    private Long id;
    private Long studentId;
    private Long courseId;
    private LocalDate enrollmentDate;
    private EnrollmentStatus status;

    /** Position in the course's waitlist queue (1 = next to be promoted). Null unless status is WAITLISTED. */
    private Integer waitlistPosition;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
