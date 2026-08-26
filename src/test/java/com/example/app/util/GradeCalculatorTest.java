package com.example.app.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GradeCalculatorTest {

    @Test
    void calculatesAPlusForHighMarks() {
        assertEquals("A+", GradeCalculator.calculate(95));
    }

    @Test
    void calculatesFForLowMarks() {
        assertEquals("F", GradeCalculator.calculate(30));
    }

    @Test
    void calculatesBoundaryValues() {
        assertEquals("A", GradeCalculator.calculate(80));
        assertEquals("C", GradeCalculator.calculate(60));
        assertEquals("D", GradeCalculator.calculate(50));
    }

    @Test
    void mapsLetterGradesToGradePoints() {
        assertEquals(4.0, GradeCalculator.gradePoint("A+"));
        assertEquals(4.0, GradeCalculator.gradePoint("A"));
        assertEquals(3.0, GradeCalculator.gradePoint("B"));
        assertEquals(2.0, GradeCalculator.gradePoint("C"));
        assertEquals(1.0, GradeCalculator.gradePoint("D"));
        assertEquals(0.5, GradeCalculator.gradePoint("E"));
        assertEquals(0.0, GradeCalculator.gradePoint("F"));
    }

    @Test
    void gradePointIsZeroForUnknownOrNullLetter() {
        assertEquals(0.0, GradeCalculator.gradePoint(null));
        assertEquals(0.0, GradeCalculator.gradePoint("Z"));
    }

    @Test
    void gradePointIsZeroForNotEligibleGrade() {
        assertEquals("NE", GradeCalculator.NOT_ELIGIBLE_GRADE);
        assertEquals(0.0, GradeCalculator.gradePoint(GradeCalculator.NOT_ELIGIBLE_GRADE));
    }
}
