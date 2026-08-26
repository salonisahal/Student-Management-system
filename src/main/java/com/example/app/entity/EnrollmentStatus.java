package com.example.app.entity;

/**
 * Lifecycle status of a student's enrollment in a course.
 */
public enum EnrollmentStatus {
    /** Student holds a confirmed seat in the course. */
    ACTIVE,
    /** The course was at capacity when the student applied; queued for the next available seat. */
    WAITLISTED,
    /** Enrollment/waitlist entry was cancelled. */
    CANCELLED
}
