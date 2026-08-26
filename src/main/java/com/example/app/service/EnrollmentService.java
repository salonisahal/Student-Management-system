package com.example.app.service;

import com.example.app.audit.AuditService;
import com.example.app.dto.EnrollmentCreateRequest;
import com.example.app.dto.EnrollmentDto;
import com.example.app.entity.*;
import com.example.app.exception.BadRequestException;
import com.example.app.exception.DuplicateResourceException;
import com.example.app.exception.ForbiddenException;
import com.example.app.exception.ResourceNotFoundException;
import com.example.app.mapper.EnrollmentMapper;
import com.example.app.repository.*;
import com.example.app.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final StudentRepository studentRepository;
    private final CourseRepository courseRepository;
    private final TeacherRepository teacherRepository;
    private final AuditService auditService;

    @Transactional
    public EnrollmentDto createEnrollment(EnrollmentCreateRequest request, Long actorId, String ipAddress) {
        Student student = studentRepository.findById(request.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + request.getStudentId()));
        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + request.getCourseId()));

        if (student.getStatus() != UserStatus.ACTIVE) {
            throw new BadRequestException("Student is not active");
        }
        if (course.getStatus() != UserStatus.ACTIVE) {
            throw new BadRequestException("Course is not active");
        }
        if (enrollmentRepository.existsByStudentIdAndCourseId(student.getId(), course.getId())) {
            throw new DuplicateResourceException("Student is already enrolled in this course");
        }

        Enrollment enrollment = new Enrollment();
        enrollment.setStudentId(student.getId());
        enrollment.setCourseId(course.getId());
        enrollment.setEnrollmentDate(LocalDate.now());
        enrollment.setStatus(UserStatus.ACTIVE);
        Enrollment saved = enrollmentRepository.save(enrollment);

        auditService.record(actorId, "ENROLLMENT_CREATE", "Enrollment", String.valueOf(saved.getId()),
                "Student " + student.getId() + " enrolled in course " + course.getId(), ipAddress);
        return EnrollmentMapper.toDto(saved);
    }

    @Transactional(readOnly = true)
    public Page<EnrollmentDto> getEnrollments(Long studentId, Long courseId, Pageable pageable, UserPrincipal principal) {
        if ("TEACHER".equals(principal.getRole())) {
            Teacher teacher = teacherRepository.findByUserId(principal.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Teacher profile not found"));
            return enrollmentRepository.searchByTeacherId(teacher.getId(), studentId, courseId, pageable).map(EnrollmentMapper::toDto);
        }
        return enrollmentRepository.search(studentId, courseId, pageable).map(EnrollmentMapper::toDto);
    }

    @Transactional(readOnly = true)
    public EnrollmentDto getEnrollment(Long id, UserPrincipal principal) {
        Enrollment enrollment = findEnrollmentOrThrow(id);
        authorizeAccess(enrollment, principal);
        return EnrollmentMapper.toDto(enrollment);
    }

    @Transactional
    public void deleteEnrollment(Long id, Long actorId, String ipAddress) {
        Enrollment enrollment = findEnrollmentOrThrow(id);
        enrollmentRepository.delete(enrollment);
        auditService.record(actorId, "ENROLLMENT_DELETE", "Enrollment", String.valueOf(id), "Enrollment removed", ipAddress);
    }

    private void authorizeAccess(Enrollment enrollment, UserPrincipal principal) {
        if ("ADMIN".equals(principal.getRole())) return;
        if ("STUDENT".equals(principal.getRole())) {
            Student student = studentRepository.findByUserId(principal.getId()).orElse(null);
            if (student == null || !student.getId().equals(enrollment.getStudentId())) {
                throw new ForbiddenException("You may only access your own enrollments");
            }
            return;
        }
        if ("TEACHER".equals(principal.getRole())) {
            Teacher teacher = teacherRepository.findByUserId(principal.getId()).orElse(null);
            Course course = courseRepository.findById(enrollment.getCourseId()).orElse(null);
            if (teacher == null || course == null || course.getTeacherId() == null
                    || !course.getTeacherId().equals(teacher.getId())) {
                throw new ForbiddenException("You are not assigned to this course");
            }
            return;
        }
        throw new ForbiddenException("Access denied");
    }

    private Enrollment findEnrollmentOrThrow(Long id) {
        return enrollmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found with id: " + id));
    }
}
