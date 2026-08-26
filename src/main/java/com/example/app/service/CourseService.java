package com.example.app.service;

import com.example.app.audit.AuditService;
import com.example.app.dto.*;
import com.example.app.entity.Course;
import com.example.app.entity.Enrollment;
import com.example.app.entity.Teacher;
import com.example.app.entity.UserStatus;
import com.example.app.exception.DuplicateResourceException;
import com.example.app.exception.ForbiddenException;
import com.example.app.exception.ResourceNotFoundException;
import com.example.app.mapper.CourseMapper;
import com.example.app.mapper.StudentMapper;
import com.example.app.repository.CourseRepository;
import com.example.app.repository.EnrollmentRepository;
import com.example.app.repository.StudentRepository;
import com.example.app.repository.TeacherRepository;
import com.example.app.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseRepository courseRepository;
    private final TeacherRepository teacherRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final StudentRepository studentRepository;
    private final AuditService auditService;

    @Transactional
    public CourseDto createCourse(CourseCreateRequest request, Long actorId, String ipAddress) {
        if (courseRepository.existsByCourseCode(request.getCourseCode())) {
            throw new DuplicateResourceException("A course with this course code already exists");
        }
        if (request.getTeacherId() != null && !teacherRepository.existsById(request.getTeacherId())) {
            throw new ResourceNotFoundException("Teacher not found with id: " + request.getTeacherId());
        }
        Course course = new Course();
        course.setCourseCode(request.getCourseCode());
        course.setCourseName(request.getCourseName());
        course.setDescription(request.getDescription());
        course.setCredits(request.getCredits());
        course.setDepartment(request.getDepartment());
        course.setTeacherId(request.getTeacherId());
        course.setMaxCapacity(request.getMaxCapacity());
        course.setStatus(UserStatus.ACTIVE);
        Course saved = courseRepository.save(course);
        auditService.record(actorId, "COURSE_CREATE", "Course", String.valueOf(saved.getId()),
                "Course created: " + saved.getCourseCode(), ipAddress);
        return CourseMapper.toDto(saved, 0L);
    }

    @Transactional(readOnly = true)
    public Page<CourseDto> getCourses(String department, UserStatus status, Long teacherId, String search, Pageable pageable) {
        return courseRepository.search(department, status, teacherId, search, pageable)
                .map(c -> CourseMapper.toDto(c, enrollmentRepository.countByCourseId(c.getId())));
    }

    @Transactional(readOnly = true)
    public CourseDto getCourse(Long id) {
        Course course = findCourseOrThrow(id);
        return CourseMapper.toDto(course, enrollmentRepository.countByCourseId(course.getId()));
    }

    @Transactional
    public CourseDto updateCourse(Long id, CourseUpdateRequest request, Long actorId, String ipAddress) {
        Course course = findCourseOrThrow(id);
        course.setCourseName(request.getCourseName());
        course.setDescription(request.getDescription());
        course.setCredits(request.getCredits());
        if (request.getDepartment() != null) course.setDepartment(request.getDepartment());
        if (request.getStatus() != null) course.setStatus(request.getStatus());
        if (request.getMaxCapacity() != null) {
            long currentEnrollment = enrollmentRepository.countByCourseId(id);
            if (request.getMaxCapacity() < currentEnrollment) {
                throw new com.example.app.exception.BadRequestException(
                        "maxCapacity (" + request.getMaxCapacity() + ") cannot be less than the current enrollment count (" + currentEnrollment + ")");
            }
            course.setMaxCapacity(request.getMaxCapacity());
        }
        Course saved = courseRepository.save(course);
        auditService.record(actorId, "COURSE_UPDATE", "Course", String.valueOf(id), "Course updated", ipAddress);
        return CourseMapper.toDto(saved, enrollmentRepository.countByCourseId(id));
    }

    @Transactional
    public void deleteCourse(Long id, Long actorId, String ipAddress) {
        Course course = findCourseOrThrow(id);
        course.setStatus(UserStatus.INACTIVE);
        courseRepository.save(course);
        auditService.record(actorId, "COURSE_DELETE", "Course", String.valueOf(id), "Course deactivated", ipAddress);
    }

    @Transactional
    public CourseDto assignTeacher(Long courseId, Long teacherId, Long actorId, String ipAddress) {
        Course course = findCourseOrThrow(courseId);
        Teacher teacher = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found with id: " + teacherId));
        course.setTeacherId(teacher.getId());
        Course saved = courseRepository.save(course);
        auditService.record(actorId, "COURSE_ASSIGN_TEACHER", "Course", String.valueOf(courseId),
                "Assigned teacher " + teacherId + " to course", ipAddress);
        return CourseMapper.toDto(saved, enrollmentRepository.countByCourseId(courseId));
    }

    @Transactional(readOnly = true)
    public List<StudentDto> getCourseStudents(Long courseId, UserPrincipal principal) {
        Course course = findCourseOrThrow(courseId);
        if ("TEACHER".equals(principal.getRole())) {
            Teacher teacher = teacherRepository.findByUserId(principal.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Teacher profile not found"));
            if (course.getTeacherId() == null || !course.getTeacherId().equals(teacher.getId())) {
                throw new ForbiddenException("You are not assigned to this course");
            }
        }
        List<Long> studentIds = enrollmentRepository.findByCourseId(courseId).stream()
                .map(Enrollment::getStudentId).collect(Collectors.toList());
        return studentRepository.findAllById(studentIds).stream().map(StudentMapper::toDto).collect(Collectors.toList());
    }

    public Course findCourseOrThrow(Long id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + id));
    }
}
