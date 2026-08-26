package com.example.app.service;

import com.example.app.audit.AuditService;
import com.example.app.dto.AttendanceCreateRequest;
import com.example.app.dto.AttendanceDto;
import com.example.app.dto.AttendanceUpdateRequest;
import com.example.app.entity.*;
import com.example.app.exception.BadRequestException;
import com.example.app.exception.DuplicateResourceException;
import com.example.app.exception.ForbiddenException;
import com.example.app.exception.ResourceNotFoundException;
import com.example.app.mapper.AttendanceMapper;
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
public class AttendanceService {

    /**
     * Minimum attendance percentage a student must have in a course before a final grade may
     * be recorded for them. Below this threshold the student must be marked "Not Eligible" (NE)
     * rather than graded normally, per institutional accreditation policy.
     */
    public static final double MIN_ATTENDANCE_PERCENTAGE_FOR_GRADING = 75.0;

    private final AttendanceRepository attendanceRepository;
    private final StudentRepository studentRepository;
    private final CourseRepository courseRepository;
    private final TeacherRepository teacherRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AuditService auditService;

    @Transactional
    public AttendanceDto createAttendance(AttendanceCreateRequest request, UserPrincipal principal, String ipAddress) {
        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + request.getCourseId()));
        studentRepository.findById(request.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + request.getStudentId()));

        if ("TEACHER".equals(principal.getRole())) {
            Teacher teacher = getTeacherByUserId(principal.getId());
            if (course.getTeacherId() == null || !course.getTeacherId().equals(teacher.getId())) {
                throw new ForbiddenException("You are not assigned to this course");
            }
        }
        if (!enrollmentRepository.existsByStudentIdAndCourseIdAndStatus(
                request.getStudentId(), request.getCourseId(), EnrollmentStatus.ACTIVE)) {
            throw new BadRequestException("Student is not actively enrolled in this course (they may be waitlisted)");
        }
        if (request.getAttendanceDate().isAfter(LocalDate.now())) {
            throw new BadRequestException("Attendance date cannot be in the future");
        }
        if (attendanceRepository.existsByStudentIdAndCourseIdAndAttendanceDate(request.getStudentId(), request.getCourseId(), request.getAttendanceDate())) {
            throw new DuplicateResourceException("Attendance record already exists for this student/course/date");
        }

        Attendance attendance = new Attendance();
        attendance.setStudentId(request.getStudentId());
        attendance.setCourseId(request.getCourseId());
        attendance.setAttendanceDate(request.getAttendanceDate());
        attendance.setStatus(request.getStatus());
        attendance.setRemarks(request.getRemarks());
        attendance.setMarkedBy(principal.getId());
        Attendance saved = attendanceRepository.save(attendance);

        auditService.record(principal.getId(), "ATTENDANCE_CREATE", "Attendance", String.valueOf(saved.getId()),
                "Attendance marked for student " + request.getStudentId(), ipAddress);
        return AttendanceMapper.toDto(saved);
    }

    @Transactional(readOnly = true)
    public Page<AttendanceDto> getAttendanceRecords(Long studentId, Long courseId, LocalDate date, LocalDate dateFrom,
                                                      LocalDate dateTo, AttendanceStatus status, Pageable pageable, UserPrincipal principal) {
        if ("TEACHER".equals(principal.getRole())) {
            Teacher teacher = getTeacherByUserId(principal.getId());
            return attendanceRepository.searchByTeacherId(teacher.getId(), studentId, courseId, date, dateFrom, dateTo, status, pageable)
                    .map(AttendanceMapper::toDto);
        }
        return attendanceRepository.search(studentId, courseId, date, dateFrom, dateTo, status, pageable).map(AttendanceMapper::toDto);
    }

    @Transactional(readOnly = true)
    public AttendanceDto getAttendance(Long id, UserPrincipal principal) {
        Attendance attendance = findAttendanceOrThrow(id);
        authorizeAccess(attendance, principal);
        return AttendanceMapper.toDto(attendance);
    }

    @Transactional
    public AttendanceDto updateAttendance(Long id, AttendanceUpdateRequest request, UserPrincipal principal, String ipAddress) {
        Attendance attendance = findAttendanceOrThrow(id);
        authorizeTeacherOrAdmin(attendance, principal);
        attendance.setStatus(request.getStatus());
        attendance.setRemarks(request.getRemarks());
        Attendance saved = attendanceRepository.save(attendance);
        auditService.record(principal.getId(), "ATTENDANCE_UPDATE", "Attendance", String.valueOf(id), "Attendance updated", ipAddress);
        return AttendanceMapper.toDto(saved);
    }

    @Transactional
    public void deleteAttendance(Long id, UserPrincipal principal, String ipAddress) {
        Attendance attendance = findAttendanceOrThrow(id);
        authorizeTeacherOrAdmin(attendance, principal);
        attendanceRepository.delete(attendance);
        auditService.record(principal.getId(), "ATTENDANCE_DELETE", "Attendance", String.valueOf(id), "Attendance deleted", ipAddress);
    }

    /**
     * Computes a student's attendance percentage for a specific course as
     * (PRESENT + LATE) / total attendance records * 100, consistent with the definition used
     * for the student-wide attendance summary.
     */
    @Transactional(readOnly = true)
    public double calculateCourseAttendancePercentage(Long studentId, Long courseId) {
        List<Attendance> records = attendanceRepository.findByStudentIdAndCourseId(studentId, courseId);
        long total = records.size();
        if (total == 0) return 0.0;
        long presentOrLate = records.stream()
                .filter(a -> a.getStatus() == AttendanceStatus.PRESENT || a.getStatus() == AttendanceStatus.LATE)
                .count();
        return Math.round((presentOrLate * 10000.0) / total) / 100.0;
    }

    /**
     * Determines whether a student is eligible for a final grade in a course based on their
     * attendance record. If no attendance has been recorded yet for the student/course, there
     * is no data to penalize against, so eligibility defaults to true. Otherwise, the student
     * must have at least {@link #MIN_ATTENDANCE_PERCENTAGE_FOR_GRADING} percent attendance.
     */
    @Transactional(readOnly = true)
    public boolean isEligibleForGrading(Long studentId, Long courseId) {
        List<Attendance> records = attendanceRepository.findByStudentIdAndCourseId(studentId, courseId);
        if (records.isEmpty()) {
            return true;
        }
        return calculateCourseAttendancePercentage(studentId, courseId) >= MIN_ATTENDANCE_PERCENTAGE_FOR_GRADING;
    }

    private void authorizeTeacherOrAdmin(Attendance attendance, UserPrincipal principal) {
        if ("ADMIN".equals(principal.getRole())) return;
        if ("TEACHER".equals(principal.getRole())) {
            Teacher teacher = getTeacherByUserId(principal.getId());
            Course course = courseRepository.findById(attendance.getCourseId()).orElse(null);
            if (course == null || course.getTeacherId() == null || !course.getTeacherId().equals(teacher.getId())) {
                throw new ForbiddenException("You are not assigned to this course");
            }
            return;
        }
        throw new ForbiddenException("Access denied");
    }

    private void authorizeAccess(Attendance attendance, UserPrincipal principal) {
        if ("ADMIN".equals(principal.getRole())) return;
        if ("STUDENT".equals(principal.getRole())) {
            var student = studentRepository.findByUserId(principal.getId()).orElse(null);
            if (student == null || !student.getId().equals(attendance.getStudentId())) {
                throw new ForbiddenException("You may only access your own attendance");
            }
            return;
        }
        authorizeTeacherOrAdmin(attendance, principal);
    }

    private Teacher getTeacherByUserId(Long userId) {
        return teacherRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher profile not found for current user"));
    }

    private Attendance findAttendanceOrThrow(Long id) {
        return attendanceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Attendance record not found with id: " + id));
    }
}
