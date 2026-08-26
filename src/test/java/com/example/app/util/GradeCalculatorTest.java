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
}
