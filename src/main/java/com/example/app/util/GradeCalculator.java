package com.example.app.util;

/**
 * Configurable grading rule used to derive a letter grade from numeric marks (0-100).
 */
public final class GradeCalculator {

    private GradeCalculator() {
    }

    public static String calculate(double marks) {
        if (marks >= 90) return "A+";
        if (marks >= 80) return "A";
        if (marks >= 70) return "B";
        if (marks >= 60) return "C";
        if (marks >= 50) return "D";
        if (marks >= 40) return "E";
        return "F";
    }
}
