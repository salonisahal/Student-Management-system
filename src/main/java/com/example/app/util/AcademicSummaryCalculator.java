package com.example.app.util;

import com.example.app.dto.GradeSummaryDto;
import com.example.app.entity.Grade;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Builds a student's academic summary, weighting each course's contribution by its credit
 * value so that, e.g., a 90 in a 1-credit elective does not count the same as a 90 in a
 * 4-credit core course.
 *
 * <p>Courses marked "Not Eligible" (NE) - because the student's attendance fell below the
 * institution's minimum threshold - are excluded from every numeric computation (averages,
 * highest/lowest marks, GPA) so an accreditation-blocking attendance issue can never be masked
 * by, or bleed into, the student's otherwise-earned academic standing.</p>
 */
public final class AcademicSummaryCalculator {

    private static final int DEFAULT_CREDITS_IF_UNKNOWN = 1;

    private AcademicSummaryCalculator() {
    }

    /**
     * @param grades             all grade records to summarize (typically for one student)
     * @param creditsByCourseId  lookup of courseId -> credit value for each course referenced
     */
    public static GradeSummaryDto summarize(List<Grade> grades, Map<Long, Integer> creditsByCourseId) {
        if (grades == null || grades.isEmpty()) {
            return new GradeSummaryDto(0, 0, 0, 0.0, 0.0, 0.0, 0.0, 0.0, "N/A");
        }

        List<Grade> eligibleGrades = grades.stream()
                .filter(g -> !GradeCalculator.NOT_ELIGIBLE_GRADE.equals(g.getGrade()))
                .collect(Collectors.toList());
        long ineligibleCourses = grades.size() - eligibleGrades.size();

        if (eligibleGrades.isEmpty()) {
            return new GradeSummaryDto(grades.size(), ineligibleCourses, 0, 0.0, 0.0, 0.0, 0.0, 0.0, "N/A");
        }

        double simpleAverageMarks = eligibleGrades.stream().mapToDouble(Grade::getMarks).average().orElse(0);
        double highest = eligibleGrades.stream().mapToDouble(Grade::getMarks).max().orElse(0);
        double lowest = eligibleGrades.stream().mapToDouble(Grade::getMarks).min().orElse(0);

        int totalCredits = 0;
        double weightedMarksSum = 0.0;
        double weightedPointsSum = 0.0;
        for (Grade grade : eligibleGrades) {
            int credits = creditsByCourseId.getOrDefault(grade.getCourseId(), DEFAULT_CREDITS_IF_UNKNOWN);
            totalCredits += credits;
            weightedMarksSum += grade.getMarks() * credits;
            weightedPointsSum += GradeCalculator.gradePoint(grade.getGrade()) * credits;
        }

        double weightedAverageMarks = totalCredits == 0 ? simpleAverageMarks : weightedMarksSum / totalCredits;
        double gpa = totalCredits == 0 ? 0.0 : weightedPointsSum / totalCredits;
        String overallGrade = GradeCalculator.calculate(weightedAverageMarks);

        return new GradeSummaryDto(
                grades.size(),
                ineligibleCourses,
                totalCredits,
                round(simpleAverageMarks),
                round(weightedAverageMarks),
                round(highest),
                round(lowest),
                round(gpa),
                overallGrade
        );
    }

    private static double round(double value) {
        return Math.round(value * 100) / 100.0;
    }
}
