package com.example.app.util;

import com.example.app.dto.GradeSummaryDto;
import com.example.app.entity.Grade;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AcademicSummaryCalculatorTest {

    private Grade grade(long courseId, double marks) {
        Grade g = new Grade();
        g.setStudentId(1L);
        g.setCourseId(courseId);
        g.setMarks(marks);
        g.setGrade(GradeCalculator.calculate(marks));
        return g;
    }

    @Test
    void returnsZeroedSummaryWhenNoGrades() {
        GradeSummaryDto summary = AcademicSummaryCalculator.summarize(List.of(), Map.of());
        assertEquals(0, summary.getTotalCourses());
        assertEquals(0, summary.getTotalCredits());
        assertEquals(0.0, summary.getGpa());
        assertEquals("N/A", summary.getOverallGrade());
    }

    @Test
    void weighsHigherCreditCoursesMoreHeavilyThanLowCreditCourses() {
        // A 90 in a 1-credit elective (course 1) and a 60 in a 4-credit core course (course 2).
        // A naive simple average would be 75, but the 4-credit course should dominate the GPA/weighted average.
        List<Grade> grades = List.of(grade(1L, 90), grade(2L, 60));
        Map<Long, Integer> credits = Map.of(1L, 1, 2L, 4);

        GradeSummaryDto summary = AcademicSummaryCalculator.summarize(grades, credits);

        assertEquals(2, summary.getTotalCourses());
        assertEquals(5, summary.getTotalCredits());
        assertEquals(75.0, summary.getAverageMarks());
        // weighted average = (90*1 + 60*4) / 5 = 66.0
        assertEquals(66.0, summary.getWeightedAverageMarks());
        // grade points: 90 -> A+ (4.0), 60 -> C (2.0); gpa = (4.0*1 + 2.0*4) / 5 = 2.4
        assertEquals(2.4, summary.getGpa());
        // overall grade should be derived from the weighted average (66 -> C), not the naive 75 average (B)
        assertEquals("C", summary.getOverallGrade());
    }

    @Test
    void fallsBackToDefaultCreditWeightWhenCourseCreditsUnknown() {
        List<Grade> grades = List.of(grade(99L, 80));
        GradeSummaryDto summary = AcademicSummaryCalculator.summarize(grades, Map.of());

        assertEquals(1, summary.getTotalCredits());
        assertEquals(80.0, summary.getWeightedAverageMarks());
    }
}
