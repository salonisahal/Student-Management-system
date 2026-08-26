package com.example.app.service;

import com.example.app.audit.AuditService;
import com.example.app.dto.CourseUpdateRequest;
import com.example.app.entity.Course;
import com.example.app.entity.EnrollmentStatus;
import com.example.app.entity.UserStatus;
import com.example.app.exception.BadRequestException;
import com.example.app.repository.CourseRepository;
import com.example.app.repository.EnrollmentRepository;
import com.example.app.repository.StudentRepository;
import com.example.app.repository.TeacherRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Verifies prerequisite validation rules on course update: a course cannot be its own
 * prerequisite, and prerequisite assignments cannot form a circular dependency.
 */
@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    @Mock
    private CourseRepository courseRepository;
    @Mock
    private TeacherRepository teacherRepository;
    @Mock
    private EnrollmentRepository enrollmentRepository;
    @Mock
    private StudentRepository studentRepository;
    @Mock
    private EnrollmentService enrollmentService;
    @Mock
    private AuditService auditService;

    @InjectMocks
    private CourseService courseService;

    private Course courseA;
    private Course courseB;

    @BeforeEach
    void setUp() {
        courseA = new Course();
        courseA.setId(1L);
        courseA.setCourseCode("CS201");
        courseA.setCourseName("Data Structures II");
        courseA.setStatus(UserStatus.ACTIVE);

        courseB = new Course();
        courseB.setId(2L);
        courseB.setCourseCode("CS101");
        courseB.setCourseName("Data Structures I");
        courseB.setStatus(UserStatus.ACTIVE);
    }

    private CourseUpdateRequest updateRequest(Set<Long> prerequisiteIds) {
        CourseUpdateRequest req = new CourseUpdateRequest();
        req.setCourseName("Data Structures II");
        req.setCredits(4);
        req.setPrerequisiteCourseIds(prerequisiteIds);
        return req;
    }

    @Test
    void rejectsSelfAsPrerequisite() {
        when(courseRepository.findById(1L)).thenReturn(Optional.of(courseA));

        assertThrows(BadRequestException.class,
                () -> courseService.updateCourse(1L, updateRequest(Set.of(1L)), 99L, "127.0.0.1"));
    }

    @Test
    void rejectsCircularPrerequisiteDependency() {
        // CS101 (courseB) already requires CS201 (courseA); attempting to make CS201 require
        // CS101 in turn would create a cycle.
        courseB.setPrerequisiteCourseIds(Set.of(1L));
        when(courseRepository.findById(1L)).thenReturn(Optional.of(courseA));
        when(courseRepository.findAllById(Set.of(2L))).thenReturn(List.of(courseB));
        when(courseRepository.findById(2L)).thenReturn(Optional.of(courseB));

        assertThrows(BadRequestException.class,
                () -> courseService.updateCourse(1L, updateRequest(Set.of(2L)), 99L, "127.0.0.1"));
    }

    @Test
    void allowsValidNonCyclicPrerequisite() {
        when(courseRepository.findById(1L)).thenReturn(Optional.of(courseA));
        when(courseRepository.findAllById(Set.of(2L))).thenReturn(List.of(courseB));
        when(courseRepository.findById(2L)).thenReturn(Optional.of(courseB));
        when(enrollmentRepository.countByCourseIdAndStatus(1L, EnrollmentStatus.ACTIVE)).thenReturn(0L);
        when(courseRepository.save(any(Course.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var dto = courseService.updateCourse(1L, updateRequest(Set.of(2L)), 99L, "127.0.0.1");

        assertEquals(Set.of(2L), dto.getPrerequisiteCourseIds());
    }
}
