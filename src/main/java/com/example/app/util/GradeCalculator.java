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

    /**
     * Configurable letter-grade -> grade-point mapping on a standard 4.0 scale.
     * Used to compute credit-weighted GPA rather than treating every course as equal weight.
     */
    public static double gradePoint(String letterGrade) {
        if (letterGrade == null) return 0.0;
        switch (letterGrade) {
            case "A+": return 4.0;
            case "A": return 4.0;
            case "B": return 3.0;
            case "C": return 2.0;
            case "D": return 1.0;
            case "E": return 0.5;
            case "F": return 0.0;
            default: return 0.0;
        }
    }
}
