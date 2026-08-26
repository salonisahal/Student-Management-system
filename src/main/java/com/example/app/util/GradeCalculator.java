package com.example.app.util;

/**
 * Configurable grading rule used to derive a letter grade from numeric marks (0-100).
 */
public final class GradeCalculator {

    /**
     * Sentinel letter grade used when a student is not allowed a final grade because their
     * attendance in the course fell below the institution's minimum eligibility threshold
     * (see {@link com.example.app.service.AttendanceService#MIN_ATTENDANCE_PERCENTAGE_FOR_GRADING}).
     * "Not Eligible" is recorded instead of a mark-derived letter grade to protect the
     * institution during accreditation audits.
     */
    public static final String NOT_ELIGIBLE_GRADE = "NE";

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
            case NOT_ELIGIBLE_GRADE: return 0.0;
            default: return 0.0;
        }
    }
}
