package com.example.app.service;

import com.example.app.audit.AuditService;
import com.example.app.dto.*;
import com.example.app.entity.Course;
import com.example.app.entity.Enrollment;
import com.example.app.entity.EnrollmentStatus;
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

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseRepository courseRepository;
    private final TeacherRepository teacherRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final StudentRepository studentRepository;
    private final EnrollmentService enrollmentService;
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
        if (request.getPrerequisiteCourseIds() != null && !request.getPrerequisiteCourseIds().isEmpty()) {
            validatePrerequisiteIdsExist(request.getPrerequisiteCourseIds());
            course.setPrerequisiteCourseIds(new HashSet<>(request.getPrerequisiteCourseIds()));
        }
        Course saved = courseRepository.save(course);
        auditService.record(actorId, "COURSE_CREATE", "Course", String.valueOf(saved.getId()),
                "Course created: " + saved.getCourseCode(), ipAddress);
        return CourseMapper.toDto(saved, 0L);
    }

    @Transactional(readOnly = true)
    public Page<CourseDto> getCourses(String department, UserStatus status, Long teacherId, String search, Pageable pageable) {
        return courseRepository.search(department, status, teacherId, search, pageable)
                .map(c -> CourseMapper.toDto(c, enrollmentRepository.countByCourseIdAndStatus(c.getId(), EnrollmentStatus.ACTIVE)));
    }

    @Transactional(readOnly = true)
    public CourseDto getCourse(Long id) {
        Course course = findCourseOrThrow(id);
        return CourseMapper.toDto(course, enrollmentRepository.countByCourseIdAndStatus(course.getId(), EnrollmentStatus.ACTIVE));
    }

    @Transactional
    public CourseDto updateCourse(Long id, CourseUpdateRequest request, Long actorId, String ipAddress) {
        Course course = findCourseOrThrow(id);
        course.setCourseName(request.getCourseName());
        course.setDescription(request.getDescription());
        course.setCredits(request.getCredits());
        if (request.getDepartment() != null) course.setDepartment(request.getDepartment());
        if (request.getStatus() != null) course.setStatus(request.getStatus());
        boolean capacityIncreased = false;
        if (request.getMaxCapacity() != null) {
            long currentEnrollment = enrollmentRepository.countByCourseIdAndStatus(id, EnrollmentStatus.ACTIVE);
            if (request.getMaxCapacity() < currentEnrollment) {
                throw new com.example.app.exception.BadRequestException(
                        "maxCapacity (" + request.getMaxCapacity() + ") cannot be less than the current enrollment count (" + currentEnrollment + ")");
            }
            capacityIncreased = course.getMaxCapacity() == null || request.getMaxCapacity() > course.getMaxCapacity();
            course.setMaxCapacity(request.getMaxCapacity());
        }
        if (request.getPrerequisiteCourseIds() != null) {
            Set<Long> newPrerequisites = new HashSet<>(request.getPrerequisiteCourseIds());
            if (!newPrerequisites.isEmpty()) {
                if (newPrerequisites.contains(id)) {
                    throw new com.example.app.exception.BadRequestException("A course cannot be its own prerequisite");
                }
                validatePrerequisiteIdsExist(newPrerequisites);
                validateNoCyclicPrerequisites(id, newPrerequisites);
            }
            course.setPrerequisiteCourseIds(newPrerequisites);
        }
        Course saved = courseRepository.save(course);
        auditService.record(actorId, "COURSE_UPDATE", "Course", String.valueOf(id), "Course updated", ipAddress);

        // If capacity grew, the newly opened seats should go to whoever has been waiting longest.
        if (capacityIncreased) {
            enrollmentService.promoteWaitlistedStudents(id, actorId, ipAddress);
        }
        return CourseMapper.toDto(saved, enrollmentRepository.countByCourseIdAndStatus(id, EnrollmentStatus.ACTIVE));
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
        return CourseMapper.toDto(saved, enrollmentRepository.countByCourseIdAndStatus(courseId, EnrollmentStatus.ACTIVE));
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
        // Only students holding a confirmed (ACTIVE) seat are part of the class roster;
        // waitlisted students are not yet officially enrolled.
        List<Long> studentIds = enrollmentRepository.findByCourseIdAndStatus(courseId, EnrollmentStatus.ACTIVE).stream()
                .map(Enrollment::getStudentId).collect(Collectors.toList());
        return studentRepository.findAllById(studentIds).stream().map(StudentMapper::toDto).collect(Collectors.toList());
    }

    public Course findCourseOrThrow(Long id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + id));
    }

    private void validatePrerequisiteIdsExist(Set<Long> prerequisiteIds) {
        List<Course> found = courseRepository.findAllById(prerequisiteIds);
        if (found.size() != prerequisiteIds.size()) {
            throw new ResourceNotFoundException("One or more prerequisite courses could not be found");
        }
    }

    /**
     * Ensures assigning {@code newPrerequisites} to course {@code courseId} does not create a
     * circular dependency (e.g. A requires B, B requires A), which would make both courses
     * permanently unenrollable.
     */
    private void validateNoCyclicPrerequisites(Long courseId, Set<Long> newPrerequisites) {
        Set<Long> visited = new HashSet<>();
        Deque<Long> toVisit = new ArrayDeque<>(newPrerequisites);
        while (!toVisit.isEmpty()) {
            Long current = toVisit.poll();
            if (current.equals(courseId)) {
                throw new com.example.app.exception.BadRequestException(
                        "Circular prerequisite dependency detected: course " + courseId + " is reachable from its own prerequisite chain");
            }
            if (!visited.add(current)) {
                continue;
            }
            courseRepository.findById(current).ifPresent(c -> {
                if (c.getPrerequisiteCourseIds() != null) {
                    toVisit.addAll(c.getPrerequisiteCourseIds());
                }
            });
        }
    }
}
