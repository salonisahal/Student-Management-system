package com.example.app.exception;

/**
 * Thrown when an operation would exceed a resource's configured capacity,
 * e.g. enrolling a student into a course that has already reached its
 * maximum number of seats.
 */
public class CapacityExceededException extends RuntimeException {
    public CapacityExceededException(String message) {
        super(message);
    }
}
