package com.example.app.service;

import com.example.app.audit.AuditService;
import com.example.app.dto.ProfileUpdateRequest;
import com.example.app.dto.UserDto;
import com.example.app.entity.Student;
import com.example.app.entity.Teacher;
import com.example.app.entity.User;
import com.example.app.exception.ResourceNotFoundException;
import com.example.app.mapper.UserMapper;
import com.example.app.repository.StudentRepository;
import com.example.app.repository.TeacherRepository;
import com.example.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public UserDto getProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return UserMapper.toDto(user);
    }

    @Transactional
    public UserDto updateProfile(Long userId, ProfileUpdateRequest request, String ipAddress) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        // Role is intentionally never modified here.
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPhone(request.getPhone());
        User saved = userRepository.save(user);

        studentRepository.findByUserId(userId).ifPresent(s -> {
            s.setFirstName(request.getFirstName());
            s.setLastName(request.getLastName());
            s.setPhone(request.getPhone());
            studentRepository.save(s);
        });
        teacherRepository.findByUserId(userId).ifPresent(t -> {
            t.setFirstName(request.getFirstName());
            t.setLastName(request.getLastName());
            t.setPhone(request.getPhone());
            teacherRepository.save(t);
        });

        auditService.record(userId, "PROFILE_UPDATE", "User", String.valueOf(userId), "Profile updated", ipAddress);
        return UserMapper.toDto(saved);
    }
}
