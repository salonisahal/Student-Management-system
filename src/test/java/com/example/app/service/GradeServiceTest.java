package com.example.app.service;

import com.example.app.audit.AuditService;
import com.example.app.dto.GradeCreateRequest;
import com.example.app.dto.GradeDto;
import com.example.app.entity.Course;
import com.example.app.entity.EnrollmentStatus;
import com.example.app.entity.Grade;
import com.example.app.entity.Role;
import com.example.app.entity.Student;
import com.example.app.entity.User;
import com.example.app.entity.UserStatus;
import com.example.app.repository.CourseRepository;
import com.example.app.repository.EnrollmentRepository;
import com.example.app.repository.GradeRepository;
import com.example.app.repository.StudentRepository;
import com.example.app.repository.TeacherRepository;
import com.example.app.security.UserPrincipal;
import com.example.app.util.GradeCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Verifies that a student's final grade is withheld ("NE" - Not Eligible) whenever their
 * attendance in the course falls below the institutional minimum, per accreditation policy.
 */
@ExtendWith(MockitoExtension.class)
class GradeServiceTest {

    @Mock
    private GradeRepository gradeRepository;
    @Mock
    private StudentRepository studentRepository;
    @Mock
    private CourseRepository courseRepository;
    @Mock
    private TeacherRepository teacherRepository;
    @Mock
    private EnrollmentRepository enrollmentRepository;
    @Mock
    private AttendanceService attendanceService;
    @Mock
    private AuditService auditService;

    @InjectMocks
    private GradeService gradeService;

    private UserPrincipal adminPrincipal;
    private Course course;
    private Student student;

    @BeforeEach
    void setUp() {
        User admin = new User();
        admin.setId(1L);
        admin.setEmail("admin@example.com");
        admin.setPassword("hashed");
        admin.setRole(Role.ADMIN);
        admin.setStatus(UserStatus.ACTIVE);
        admin.setCreatedAt(LocalDateTime.now());
        admin.setUpdatedAt(LocalDateTime.now());
        adminPrincipal = new UserPrincipal(admin);

        course = new Course();
        course.setId(10L);
        course.setCourseCode("CS101");
        course.setCourseName("Intro to CS");
        course.setCredits(3);

        student = new Student();
        student.setId(5L);

        when(courseRepository.findById(10L)).thenReturn(Optional.of(course));
        when(studentRepository.findById(5L)).thenReturn(Optional.of(student));
        when(enrollmentRepository.existsByStudentIdAndCourseIdAndStatus(5L, 10L, EnrollmentStatus.ACTIVE))
                .thenReturn(true);
        when(gradeRepository.save(any(Grade.class))).thenAnswer(invocation -> {
            Grade g = invocation.getArgument(0);
            g.setId(100L);
            return g;
        });
    }

    private GradeCreateRequest request(double marks) {
        GradeCreateRequest req = new GradeCreateRequest();
        req.setStudentId(5L);
        req.setCourseId(10L);
        req.setMarks(marks);
        return req;
    }

    @Test
    void marksGradeAsNotEligibleWhenAttendanceBelowThreshold() {
        when(attendanceService.isEligibleForGrading(5L, 10L)).thenReturn(false);

        GradeDto result = gradeService.createGrade(request(92.0), adminPrincipal, "127.0.0.1");

        assertEquals(GradeCalculator.NOT_ELIGIBLE_GRADE, result.getGrade());
        // Marks are still recorded even though the letter grade is withheld.
        assertEquals(92.0, result.getMarks());
    }

    @Test
    void computesNormalLetterGradeWhenAttendanceMeetsThreshold() {
        when(attendanceService.isEligibleForGrading(5L, 10L)).thenReturn(true);

        GradeDto result = gradeService.createGrade(request(92.0), adminPrincipal, "127.0.0.1");

        assertEquals("A+", result.getGrade());
    }

    @Test
    void auditLogNotesInsufficientAttendanceWhenIneligible() {
        when(attendanceService.isEligibleForGrading(5L, 10L)).thenReturn(false);

        gradeService.createGrade(request(60.0), adminPrincipal, "127.0.0.1");

        ArgumentCaptor<String> descriptionCaptor = ArgumentCaptor.forClass(String.class);
        org.mockito.Mockito.verify(auditService).record(any(), anyString(), anyString(), anyString(),
                descriptionCaptor.capture(), anyString());
        assertEquals(true, descriptionCaptor.getValue().contains("Not Eligible"));
    }
}
