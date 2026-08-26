package com.example.app.service;

import com.example.app.audit.AuditService;
import com.example.app.dto.GradeCreateRequest;
import com.example.app.dto.GradeDto;
import com.example.app.dto.GradeUpdateRequest;
import com.example.app.entity.Course;
import com.example.app.entity.EnrollmentStatus;
import com.example.app.entity.Grade;
import com.example.app.entity.Teacher;
import com.example.app.exception.BadRequestException;
import com.example.app.exception.ForbiddenException;
import com.example.app.exception.ResourceNotFoundException;
import com.example.app.mapper.GradeMapper;
import com.example.app.repository.CourseRepository;
import com.example.app.repository.EnrollmentRepository;
import com.example.app.repository.GradeRepository;
import com.example.app.repository.StudentRepository;
import com.example.app.repository.TeacherRepository;
import com.example.app.security.UserPrincipal;
import com.example.app.util.GradeCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GradeService {

    private final GradeRepository gradeRepository;
    private final StudentRepository studentRepository;
    private final CourseRepository courseRepository;
    private final TeacherRepository teacherRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AuditService auditService;

    @Transactional
    public GradeDto createGrade(GradeCreateRequest request, UserPrincipal principal, String ipAddress) {
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
        if (request.getMarks() < 0 || request.getMarks() > 100) {
            throw new BadRequestException("Marks must be between 0 and 100");
        }

        Grade grade = new Grade();
        grade.setStudentId(request.getStudentId());
        grade.setCourseId(request.getCourseId());
        grade.setMarks(request.getMarks());
        grade.setGrade(GradeCalculator.calculate(request.getMarks()));
        grade.setRemarks(request.getRemarks());
        grade.setGradedBy(principal.getId());
        Grade saved = gradeRepository.save(grade);

        auditService.record(principal.getId(), "GRADE_CREATE", "Grade", String.valueOf(saved.getId()),
                "Grade recorded for student " + request.getStudentId(), ipAddress);
        return GradeMapper.toDto(saved);
    }

    @Transactional(readOnly = true)
    public Page<GradeDto> getGrades(Long studentId, Long courseId, String gradeLetter, Double minMarks, Double maxMarks,
                                      Pageable pageable, UserPrincipal principal) {
        if ("TEACHER".equals(principal.getRole())) {
            Teacher teacher = getTeacherByUserId(principal.getId());
            return gradeRepository.searchByTeacherId(teacher.getId(), studentId, courseId, gradeLetter, minMarks, maxMarks, pageable)
                    .map(GradeMapper::toDto);
        }
        return gradeRepository.search(studentId, courseId, gradeLetter, minMarks, maxMarks, pageable).map(GradeMapper::toDto);
    }

    @Transactional(readOnly = true)
    public GradeDto getGrade(Long id, UserPrincipal principal) {
        Grade grade = findGradeOrThrow(id);
        authorizeAccess(grade, principal);
        return GradeMapper.toDto(grade);
    }

    @Transactional
    public GradeDto updateGrade(Long id, GradeUpdateRequest request, UserPrincipal principal, String ipAddress) {
        Grade grade = findGradeOrThrow(id);
        authorizeTeacherOrAdmin(grade, principal);
        if (request.getMarks() < 0 || request.getMarks() > 100) {
            throw new BadRequestException("Marks must be between 0 and 100");
        }
        grade.setMarks(request.getMarks());
        grade.setGrade(GradeCalculator.calculate(request.getMarks()));
        grade.setRemarks(request.getRemarks());
        grade.setGradedBy(principal.getId());
        Grade saved = gradeRepository.save(grade);
        auditService.record(principal.getId(), "GRADE_UPDATE", "Grade", String.valueOf(id), "Grade updated", ipAddress);
        return GradeMapper.toDto(saved);
    }

    @Transactional
    public void deleteGrade(Long id, Long actorId, String ipAddress) {
        Grade grade = findGradeOrThrow(id);
        gradeRepository.delete(grade);
        auditService.record(actorId, "GRADE_DELETE", "Grade", String.valueOf(id), "Grade deleted", ipAddress);
    }

    private void authorizeTeacherOrAdmin(Grade grade, UserPrincipal principal) {
        if ("ADMIN".equals(principal.getRole())) return;
        if ("TEACHER".equals(principal.getRole())) {
            Teacher teacher = getTeacherByUserId(principal.getId());
            Course course = courseRepository.findById(grade.getCourseId()).orElse(null);
            if (course == null || course.getTeacherId() == null || !course.getTeacherId().equals(teacher.getId())) {
                throw new ForbiddenException("You are not assigned to this course");
            }
            return;
        }
        throw new ForbiddenException("Access denied");
    }

    private void authorizeAccess(Grade grade, UserPrincipal principal) {
        if ("ADMIN".equals(principal.getRole())) return;
        if ("STUDENT".equals(principal.getRole())) {
            var student = studentRepository.findByUserId(principal.getId()).orElse(null);
            if (student == null || !student.getId().equals(grade.getStudentId())) {
                throw new ForbiddenException("You may only access your own grades");
            }
            return;
        }
        authorizeTeacherOrAdmin(grade, principal);
    }

    private Teacher getTeacherByUserId(Long userId) {
        return teacherRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher profile not found for current user"));
    }

    private Grade findGradeOrThrow(Long id) {
        return gradeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Grade not found with id: " + id));
    }
}
