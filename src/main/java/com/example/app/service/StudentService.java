package com.example.app.service;

import com.example.app.audit.AuditService;
import com.example.app.dto.*;
import com.example.app.entity.*;
import com.example.app.exception.DuplicateResourceException;
import com.example.app.exception.ForbiddenException;
import com.example.app.exception.ResourceNotFoundException;
import com.example.app.mapper.CourseMapper;
import com.example.app.mapper.StudentMapper;
import com.example.app.repository.*;
import com.example.app.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudentService {

    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final TeacherRepository teacherRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AttendanceRepository attendanceRepository;
    private final GradeRepository gradeRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    @Transactional
    public StudentDto createStudent(StudentCreateRequest request, Long actorId, String ipAddress) {
        if (studentRepository.existsByStudentNumber(request.getStudentNumber())) {
            throw new DuplicateResourceException("A student with this student number already exists");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("A user with this email already exists");
        }

        User user = new User();
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setPhone(request.getPhone());
        user.setRole(Role.STUDENT);
        user.setStatus(UserStatus.ACTIVE);
        User savedUser = userRepository.save(user);

        Student student = new Student();
        student.setUserId(savedUser.getId());
        student.setStudentNumber(request.getStudentNumber());
        student.setFirstName(request.getFirstName());
        student.setLastName(request.getLastName());
        student.setEmail(request.getEmail());
        student.setPhone(request.getPhone());
        student.setDateOfBirth(request.getDateOfBirth());
        student.setGender(request.getGender());
        student.setAddress(request.getAddress());
        student.setDepartment(request.getDepartment());
        student.setAdmissionDate(request.getAdmissionDate());
        student.setStatus(UserStatus.ACTIVE);
        Student saved = studentRepository.save(student);

        auditService.record(actorId, "STUDENT_CREATE", "Student", String.valueOf(saved.getId()),
                "Student created: " + saved.getStudentNumber(), ipAddress);
        return StudentMapper.toDto(saved);
    }

    @Transactional(readOnly = true)
    public Page<StudentDto> getStudents(String department, UserStatus status, String search, Pageable pageable, UserPrincipal principal) {
        if ("TEACHER".equals(principal.getRole())) {
            Teacher teacher = getTeacherByUserId(principal.getId());
            return studentRepository.searchByTeacherId(teacher.getId(), department, status, search, pageable)
                    .map(StudentMapper::toDto);
        }
        return studentRepository.search(department, status, search, pageable).map(StudentMapper::toDto);
    }

    @Transactional(readOnly = true)
    public StudentDto getStudent(Long id, UserPrincipal principal) {
        Student student = findStudentOrThrow(id);
        authorizeAccess(student, principal);
        return StudentMapper.toDto(student);
    }

    @Transactional
    public StudentDto updateStudent(Long id, StudentUpdateRequest request, UserPrincipal principal, String ipAddress) {
        Student student = findStudentOrThrow(id);
        boolean isAdmin = "ADMIN".equals(principal.getRole());
        boolean isOwner = "STUDENT".equals(principal.getRole()) && student.getUserId().equals(principal.getId());

        if (!isAdmin && !isOwner) {
            throw new ForbiddenException("You are not allowed to update this student");
        }

        student.setFirstName(request.getFirstName());
        student.setLastName(request.getLastName());
        student.setEmail(request.getEmail());
        student.setPhone(request.getPhone());
        student.setDateOfBirth(request.getDateOfBirth());
        student.setGender(request.getGender());
        student.setAddress(request.getAddress());

        // Administrative fields may only be changed by ADMIN
        if (isAdmin) {
            if (request.getDepartment() != null) student.setDepartment(request.getDepartment());
            if (request.getStudentNumber() != null) student.setStudentNumber(request.getStudentNumber());
            if (request.getStatus() != null) student.setStatus(request.getStatus());
        }

        Student saved = studentRepository.save(student);
        auditService.record(principal.getId(), "STUDENT_UPDATE", "Student", String.valueOf(id), "Student updated", ipAddress);
        return StudentMapper.toDto(saved);
    }

    @Transactional
    public void deleteStudent(Long id, Long actorId, String ipAddress) {
        Student student = findStudentOrThrow(id);
        student.setStatus(UserStatus.INACTIVE);
        studentRepository.save(student);
        userRepository.findById(student.getUserId()).ifPresent(u -> {
            u.setStatus(UserStatus.INACTIVE);
            userRepository.save(u);
        });
        auditService.record(actorId, "STUDENT_DELETE", "Student", String.valueOf(id), "Student deactivated", ipAddress);
    }

    @Transactional(readOnly = true)
    public List<CourseDto> getStudentCourses(Long id, UserPrincipal principal) {
        Student student = findStudentOrThrow(id);
        authorizeAccess(student, principal);
        // Only ACTIVE enrollments represent courses the student is actually attending;
        // WAITLISTED entries are not yet confirmed seats.
        List<Long> courseIds = enrollmentRepository.findByStudentId(id).stream()
                .filter(e -> e.getStatus() == EnrollmentStatus.ACTIVE)
                .map(Enrollment::getCourseId).collect(Collectors.toList());
        return courseRepository.findAllById(courseIds).stream().map(CourseMapper::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<EnrollmentDto> getStudentEnrollments(Long id, UserPrincipal principal) {
        Student student = findStudentOrThrow(id);
        authorizeAccess(student, principal);
        return enrollmentRepository.findByStudentId(id).stream()
                .map(e -> {
                    Integer position = null;
                    if (e.getStatus() == EnrollmentStatus.WAITLISTED) {
                        position = (int) enrollmentRepository.countByCourseIdAndStatusAndIdLessThan(
                                e.getCourseId(), EnrollmentStatus.WAITLISTED, e.getId()) + 1;
                    }
                    return com.example.app.mapper.EnrollmentMapper.toDto(e, position);
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AttendanceDto> getStudentAttendance(Long id, UserPrincipal principal) {
        Student student = findStudentOrThrow(id);
        authorizeAccess(student, principal);
        return attendanceRepository.findByStudentId(id).stream()
                .map(com.example.app.mapper.AttendanceMapper::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<GradeDto> getStudentGrades(Long id, UserPrincipal principal) {
        Student student = findStudentOrThrow(id);
        authorizeAccess(student, principal);
        return gradeRepository.findByStudentId(id).stream()
                .map(com.example.app.mapper.GradeMapper::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AttendanceSummaryDto getAttendanceSummary(Long id, UserPrincipal principal) {
        Student student = findStudentOrThrow(id);
        authorizeAccess(student, principal);
        List<Attendance> records = attendanceRepository.findByStudentId(id);
        long total = records.size();
        long present = records.stream().filter(a -> a.getStatus() == AttendanceStatus.PRESENT).count();
        long absent = records.stream().filter(a -> a.getStatus() == AttendanceStatus.ABSENT).count();
        long late = records.stream().filter(a -> a.getStatus() == AttendanceStatus.LATE).count();
        long excused = records.stream().filter(a -> a.getStatus() == AttendanceStatus.EXCUSED).count();
        double percentage = total == 0 ? 0.0 : ((present + late) * 100.0) / total;
        return new AttendanceSummaryDto(total, present, absent, late, excused, Math.round(percentage * 100) / 100.0);
    }

    @Transactional(readOnly = true)
    public GradeSummaryDto getGradeSummary(Long id, UserPrincipal principal) {
        Student student = findStudentOrThrow(id);
        authorizeAccess(student, principal);
        List<Grade> grades = gradeRepository.findByStudentId(id);
        Map<Long, Integer> creditsByCourseId = courseRepository.findAllById(
                        grades.stream().map(Grade::getCourseId).distinct().collect(Collectors.toList()))
                .stream().collect(Collectors.toMap(Course::getId, Course::getCredits));
        return com.example.app.util.AcademicSummaryCalculator.summarize(grades, creditsByCourseId);
    }

    // ---- Authorization helpers ----

    public void authorizeAccess(Student student, UserPrincipal principal) {
        if ("ADMIN".equals(principal.getRole())) return;
        if ("STUDENT".equals(principal.getRole())) {
            if (!student.getUserId().equals(principal.getId())) {
                throw new ForbiddenException("You may only access your own student record");
            }
            return;
        }
        if ("TEACHER".equals(principal.getRole())) {
            Teacher teacher = getTeacherByUserId(principal.getId());
            if (!isTeacherAuthorizedForStudent(teacher.getId(), student.getId())) {
                throw new ForbiddenException("You are not assigned to a course this student is enrolled in");
            }
            return;
        }
        throw new ForbiddenException("Access denied");
    }

    public boolean isTeacherAuthorizedForStudent(Long teacherId, Long studentId) {
        List<Long> teacherCourseIds = courseRepository.findByTeacherId(teacherId).stream()
                .map(Course::getId).collect(Collectors.toList());
        if (teacherCourseIds.isEmpty()) return false;
        return enrollmentRepository.findByStudentId(studentId).stream()
                .anyMatch(e -> teacherCourseIds.contains(e.getCourseId()));
    }

    private Teacher getTeacherByUserId(Long userId) {
        return teacherRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher profile not found for current user"));
    }

    private Student findStudentOrThrow(Long id) {
        return studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + id));
    }
}
