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
import java.util.List;

@Service
@RequiredArgsConstructor
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final StudentRepository studentRepository;
    private final CourseRepository courseRepository;
    private final TeacherRepository teacherRepository;
    private final GradeService gradeService;
    private final AuditService auditService;

    /**
     * Enroll a student in a course. If the course is already at capacity the student is placed
     * on the waitlist (status WAITLISTED) instead of being rejected outright; waitlisted
     * students are automatically promoted to ACTIVE, in first-come-first-served order, whenever
     * a seat frees up (see {@link #promoteWaitlistedStudents}).
     */
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

        // A student must have already passed every prerequisite course before they can be
        // enrolled (or even waitlisted) into this course.
        if (course.getPrerequisiteCourseIds() != null && !course.getPrerequisiteCourseIds().isEmpty()) {
            List<Long> unmetPrerequisiteIds = course.getPrerequisiteCourseIds().stream()
                    .filter(prereqId -> !gradeService.hasPassedCourse(student.getId(), prereqId))
                    .collect(java.util.stream.Collectors.toList());
            if (!unmetPrerequisiteIds.isEmpty()) {
                List<String> unmetCourseCodes = courseRepository.findAllById(unmetPrerequisiteIds).stream()
                        .map(Course::getCourseCode)
                        .collect(java.util.stream.Collectors.toList());
                throw new BadRequestException(
                        "Student has not passed the required prerequisite course(s): " + unmetCourseCodes);
            }
        }

        // Re-fetch the course with a row-level lock so that concurrent enrollment requests
        // for the same course are serialized and cannot both slip past the capacity check.
        Course lockedCourse = courseRepository.findByIdForUpdate(course.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + course.getId()));

        boolean seatAvailable = true;
        if (lockedCourse.getMaxCapacity() != null) {
            long currentActiveEnrollment = enrollmentRepository.countByCourseIdAndStatus(lockedCourse.getId(), EnrollmentStatus.ACTIVE);
            seatAvailable = currentActiveEnrollment < lockedCourse.getMaxCapacity();
        }

        Enrollment enrollment = new Enrollment();
        enrollment.setStudentId(student.getId());
        enrollment.setCourseId(course.getId());
        enrollment.setEnrollmentDate(LocalDate.now());

        Integer waitlistPosition = null;
        if (seatAvailable) {
            enrollment.setStatus(EnrollmentStatus.ACTIVE);
        } else {
            enrollment.setStatus(EnrollmentStatus.WAITLISTED);
            waitlistPosition = (int) enrollmentRepository.countByCourseIdAndStatus(lockedCourse.getId(), EnrollmentStatus.WAITLISTED) + 1;
        }

        Enrollment saved = enrollmentRepository.save(enrollment);

        if (seatAvailable) {
            auditService.record(actorId, "ENROLLMENT_CREATE", "Enrollment", String.valueOf(saved.getId()),
                    "Student " + student.getId() + " enrolled in course " + course.getId(), ipAddress);
        } else {
            auditService.record(actorId, "ENROLLMENT_WAITLISTED", "Enrollment", String.valueOf(saved.getId()),
                    "Course " + course.getId() + " is at capacity; student " + student.getId()
                            + " placed on waitlist at position " + waitlistPosition, ipAddress);
        }
        return EnrollmentMapper.toDto(saved, waitlistPosition);
    }

    @Transactional(readOnly = true)
    public Page<EnrollmentDto> getEnrollments(Long studentId, Long courseId, EnrollmentStatus status, Pageable pageable, UserPrincipal principal) {
        if ("TEACHER".equals(principal.getRole())) {
            Teacher teacher = teacherRepository.findByUserId(principal.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Teacher profile not found"));
            return enrollmentRepository.searchByTeacherId(teacher.getId(), studentId, courseId, status, pageable)
                    .map(this::toDtoWithPosition);
        }
        return enrollmentRepository.search(studentId, courseId, status, pageable).map(this::toDtoWithPosition);
    }

    @Transactional(readOnly = true)
    public EnrollmentDto getEnrollment(Long id, UserPrincipal principal) {
        Enrollment enrollment = findEnrollmentOrThrow(id);
        authorizeAccess(enrollment, principal);
        return toDtoWithPosition(enrollment);
    }

    /**
     * Remove/cancel an enrollment. If the removed enrollment held an active seat, the course's
     * waitlist (if any) is automatically consulted and the longest-waiting student promoted.
     */
    @Transactional
    public void deleteEnrollment(Long id, Long actorId, String ipAddress) {
        Enrollment enrollment = findEnrollmentOrThrow(id);
        Long courseId = enrollment.getCourseId();
        Long studentId = enrollment.getStudentId();
        boolean freedActiveSeat = enrollment.getStatus() == EnrollmentStatus.ACTIVE;

        enrollmentRepository.delete(enrollment);
        auditService.record(actorId, "ENROLLMENT_DELETE", "Enrollment", String.valueOf(id),
                "Enrollment removed for student " + studentId + " in course " + courseId, ipAddress);

        if (freedActiveSeat) {
            promoteWaitlistedStudents(courseId, actorId, ipAddress);
        }
    }

    /**
     * Promotes as many waitlisted students as there are free seats in the course, oldest
     * waitlist entry first. Safe to call whenever a seat may have freed up (a drop) or capacity
     * may have increased (an admin raising maxCapacity). No-op if there is no waitlist or no
     * free seats.
     */
    @Transactional
    public int promoteWaitlistedStudents(Long courseId, Long actorId, String ipAddress) {
        Course course = courseRepository.findByIdForUpdate(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + courseId));
        if (course.getStatus() != UserStatus.ACTIVE) {
            return 0;
        }

        int promoted = 0;
        while (true) {
            if (course.getMaxCapacity() != null) {
                long activeCount = enrollmentRepository.countByCourseIdAndStatus(courseId, EnrollmentStatus.ACTIVE);
                if (activeCount >= course.getMaxCapacity()) {
                    break;
                }
            }
            Enrollment next = enrollmentRepository
                    .findFirstByCourseIdAndStatusOrderByIdAsc(courseId, EnrollmentStatus.WAITLISTED)
                    .orElse(null);
            if (next == null) {
                break;
            }
            next.setStatus(EnrollmentStatus.ACTIVE);
            next.setEnrollmentDate(LocalDate.now());
            enrollmentRepository.save(next);
            auditService.record(actorId, "ENROLLMENT_PROMOTED", "Enrollment", String.valueOf(next.getId()),
                    "Student " + next.getStudentId() + " promoted from waitlist to an active seat in course " + courseId, ipAddress);
            promoted++;
        }
        return promoted;
    }

    private EnrollmentDto toDtoWithPosition(Enrollment enrollment) {
        Integer position = null;
        if (enrollment.getStatus() == EnrollmentStatus.WAITLISTED) {
            position = (int) enrollmentRepository.countByCourseIdAndStatusAndIdLessThan(
                    enrollment.getCourseId(), EnrollmentStatus.WAITLISTED, enrollment.getId()) + 1;
        }
        return EnrollmentMapper.toDto(enrollment, position);
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
