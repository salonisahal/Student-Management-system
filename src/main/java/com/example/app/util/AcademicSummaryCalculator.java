package com.example.app.util;

import com.example.app.dto.GradeSummaryDto;
import com.example.app.entity.Grade;

import java.util.List;
import java.util.Map;

/**
 * Builds a student's academic summary, weighting each course's contribution by its credit
 * value so that, e.g., a 90 in a 1-credit elective does not count the same as a 90 in a
 * 4-credit core course.
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
            return new GradeSummaryDto(0, 0, 0.0, 0.0, 0.0, 0.0, 0.0, "N/A");
        }

        double simpleAverageMarks = grades.stream().mapToDouble(Grade::getMarks).average().orElse(0);
        double highest = grades.stream().mapToDouble(Grade::getMarks).max().orElse(0);
        double lowest = grades.stream().mapToDouble(Grade::getMarks).min().orElse(0);

        int totalCredits = 0;
        double weightedMarksSum = 0.0;
        double weightedPointsSum = 0.0;
        for (Grade grade : grades) {
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
