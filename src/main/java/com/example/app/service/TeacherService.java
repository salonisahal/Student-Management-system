package com.example.app.service;

import com.example.app.audit.AuditService;
import com.example.app.dto.*;
import com.example.app.entity.Role;
import com.example.app.entity.Teacher;
import com.example.app.entity.User;
import com.example.app.entity.UserStatus;
import com.example.app.exception.DuplicateResourceException;
import com.example.app.exception.ForbiddenException;
import com.example.app.exception.ResourceNotFoundException;
import com.example.app.mapper.CourseMapper;
import com.example.app.mapper.TeacherMapper;
import com.example.app.repository.CourseRepository;
import com.example.app.repository.TeacherRepository;
import com.example.app.repository.UserRepository;
import com.example.app.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TeacherService {

    private final TeacherRepository teacherRepository;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    @Transactional
    public TeacherDto createTeacher(TeacherCreateRequest request, Long actorId, String ipAddress) {
        if (teacherRepository.existsByEmployeeNumber(request.getEmployeeNumber())) {
            throw new DuplicateResourceException("A teacher with this employee number already exists");
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
        user.setRole(Role.TEACHER);
        user.setStatus(UserStatus.ACTIVE);
        User savedUser = userRepository.save(user);

        Teacher teacher = new Teacher();
        teacher.setUserId(savedUser.getId());
        teacher.setEmployeeNumber(request.getEmployeeNumber());
        teacher.setFirstName(request.getFirstName());
        teacher.setLastName(request.getLastName());
        teacher.setEmail(request.getEmail());
        teacher.setPhone(request.getPhone());
        teacher.setDepartment(request.getDepartment());
        teacher.setJoiningDate(request.getJoiningDate());
        teacher.setStatus(UserStatus.ACTIVE);
        Teacher saved = teacherRepository.save(teacher);

        auditService.record(actorId, "TEACHER_CREATE", "Teacher", String.valueOf(saved.getId()),
                "Teacher created: " + saved.getEmployeeNumber(), ipAddress);
        return TeacherMapper.toDto(saved);
    }

    @Transactional(readOnly = true)
    public Page<TeacherDto> getTeachers(String search, Pageable pageable) {
        return teacherRepository.search(search, pageable).map(TeacherMapper::toDto);
    }

    @Transactional(readOnly = true)
    public TeacherDto getTeacher(Long id) {
        return TeacherMapper.toDto(findTeacherOrThrow(id));
    }

    @Transactional
    public TeacherDto updateTeacher(Long id, TeacherUpdateRequest request, UserPrincipal principal, String ipAddress) {
        Teacher teacher = findTeacherOrThrow(id);
        boolean isAdmin = "ADMIN".equals(principal.getRole());
        boolean isOwner = "TEACHER".equals(principal.getRole()) && teacher.getUserId().equals(principal.getId());
        if (!isAdmin && !isOwner) {
            throw new ForbiddenException("You are not allowed to update this teacher");
        }
        teacher.setFirstName(request.getFirstName());
        teacher.setLastName(request.getLastName());
        teacher.setEmail(request.getEmail());
        teacher.setPhone(request.getPhone());
        if (isAdmin) {
            if (request.getDepartment() != null) teacher.setDepartment(request.getDepartment());
            if (request.getStatus() != null) teacher.setStatus(request.getStatus());
        }
        Teacher saved = teacherRepository.save(teacher);
        auditService.record(principal.getId(), "TEACHER_UPDATE", "Teacher", String.valueOf(id), "Teacher updated", ipAddress);
        return TeacherMapper.toDto(saved);
    }

    @Transactional
    public void deleteTeacher(Long id, Long actorId, String ipAddress) {
        Teacher teacher = findTeacherOrThrow(id);
        teacher.setStatus(UserStatus.INACTIVE);
        teacherRepository.save(teacher);
        userRepository.findById(teacher.getUserId()).ifPresent(u -> {
            u.setStatus(UserStatus.INACTIVE);
            userRepository.save(u);
        });
        auditService.record(actorId, "TEACHER_DELETE", "Teacher", String.valueOf(id), "Teacher deactivated", ipAddress);
    }

    @Transactional(readOnly = true)
    public List<CourseDto> getTeacherCourses(Long id, UserPrincipal principal) {
        Teacher teacher = findTeacherOrThrow(id);
        boolean isAdmin = "ADMIN".equals(principal.getRole());
        boolean isOwner = "TEACHER".equals(principal.getRole()) && teacher.getUserId().equals(principal.getId());
        if (!isAdmin && !isOwner) {
            throw new ForbiddenException("You may only view your own assigned courses");
        }
        return courseRepository.findByTeacherId(id).stream().map(CourseMapper::toDto).collect(Collectors.toList());
    }

    public Teacher findTeacherOrThrow(Long id) {
        return teacherRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found with id: " + id));
    }
}
