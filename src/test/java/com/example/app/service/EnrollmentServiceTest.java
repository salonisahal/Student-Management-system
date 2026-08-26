package com.example.app.service;

import com.example.app.audit.AuditService;
import com.example.app.dto.EnrollmentCreateRequest;
import com.example.app.dto.EnrollmentDto;
import com.example.app.entity.Course;
import com.example.app.entity.Enrollment;
import com.example.app.entity.EnrollmentStatus;
import com.example.app.entity.Student;
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

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Verifies that a student cannot enroll (or be waitlisted) in a course that has prerequisite
 * courses unless they have already passed every one of them.
 */
@ExtendWith(MockitoExtension.class)
class EnrollmentServiceTest {

    @Mock
    private EnrollmentRepository enrollmentRepository;
    @Mock
    private StudentRepository studentRepository;
    @Mock
    private CourseRepository courseRepository;
    @Mock
    private TeacherRepository teacherRepository;
    @Mock
    private GradeService gradeService;
    @Mock
    private AuditService auditService;

    @InjectMocks
    private EnrollmentService enrollmentService;

    private Student student;
    private Course advancedCourse;
    private Course prerequisiteCourse;

    @BeforeEach
    void setUp() {
        student = new Student();
        student.setId(5L);
        student.setStatus(UserStatus.ACTIVE);

        prerequisiteCourse = new Course();
        prerequisiteCourse.setId(1L);
        prerequisiteCourse.setCourseCode("CS101");
        prerequisiteCourse.setCourseName("Data Structures I");
        prerequisiteCourse.setStatus(UserStatus.ACTIVE);

        advancedCourse = new Course();
        advancedCourse.setId(2L);
        advancedCourse.setCourseCode("CS201");
        advancedCourse.setCourseName("Data Structures II");
        advancedCourse.setStatus(UserStatus.ACTIVE);
        advancedCourse.setPrerequisiteCourseIds(Set.of(1L));

        when(studentRepository.findById(5L)).thenReturn(Optional.of(student));
        when(courseRepository.findById(2L)).thenReturn(Optional.of(advancedCourse));
        when(enrollmentRepository.existsByStudentIdAndCourseId(5L, 2L)).thenReturn(false);
    }

    private EnrollmentCreateRequest request() {
        EnrollmentCreateRequest req = new EnrollmentCreateRequest();
        req.setStudentId(5L);
        req.setCourseId(2L);
        return req;
    }

    @Test
    void rejectsEnrollmentWhenPrerequisiteNotPassed() {
        when(gradeService.hasPassedCourse(5L, 1L)).thenReturn(false);
        when(courseRepository.findAllById(any())).thenReturn(java.util.List.of(prerequisiteCourse));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> enrollmentService.createEnrollment(request(), 1L, "127.0.0.1"));

        assertEquals(true, ex.getMessage().contains("CS101"));
    }

    @Test
    void allowsEnrollmentWhenPrerequisitePassed() {
        when(gradeService.hasPassedCourse(5L, 1L)).thenReturn(true);
        when(courseRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(advancedCourse));
        when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(invocation -> {
            Enrollment e = invocation.getArgument(0);
            e.setId(50L);
            return e;
        });

        EnrollmentDto dto = enrollmentService.createEnrollment(request(), 1L, "127.0.0.1");

        assertEquals(EnrollmentStatus.ACTIVE, dto.getStatus());
    }
}
