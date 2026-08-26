package com.example.app.service;

import com.example.app.audit.AuditService;
import com.example.app.dto.UserCreateRequest;
import com.example.app.dto.UserDto;
import com.example.app.dto.UserStatusUpdateRequest;
import com.example.app.dto.UserUpdateRequest;
import com.example.app.entity.Role;
import com.example.app.entity.User;
import com.example.app.entity.UserStatus;
import com.example.app.exception.DuplicateResourceException;
import com.example.app.exception.ResourceNotFoundException;
import com.example.app.mapper.UserMapper;
import com.example.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    @Transactional
    public UserDto createUser(UserCreateRequest request, Long actorId, String ipAddress) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("A user with this email already exists");
        }
        User user = new User();
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setPhone(request.getPhone());
        user.setRole(request.getRole());
        user.setStatus(UserStatus.ACTIVE);
        User saved = userRepository.save(user);
        auditService.record(actorId, "USER_CREATE", "User", String.valueOf(saved.getId()), "User created by admin", ipAddress);
        return UserMapper.toDto(saved);
    }

    @Transactional(readOnly = true)
    public Page<UserDto> getUsers(Role role, UserStatus status, String search, Pageable pageable) {
        return userRepository.search(role, status, search, pageable).map(UserMapper::toDto);
    }

    @Transactional(readOnly = true)
    public UserDto getUser(Long id) {
        return UserMapper.toDto(findUserOrThrow(id));
    }

    @Transactional
    public UserDto updateUser(Long id, UserUpdateRequest request, Long actorId, String ipAddress) {
        User user = findUserOrThrow(id);
        if (!user.getEmail().equals(request.getEmail()) && userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("A user with this email already exists");
        }
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        User saved = userRepository.save(user);
        auditService.record(actorId, "USER_UPDATE", "User", String.valueOf(id), "User updated by admin", ipAddress);
        return UserMapper.toDto(saved);
    }

    @Transactional
    public UserDto updateStatus(Long id, UserStatusUpdateRequest request, Long actorId, String ipAddress) {
        User user = findUserOrThrow(id);
        user.setStatus(request.getStatus());
        User saved = userRepository.save(user);
        auditService.record(actorId, "USER_STATUS_UPDATE", "User", String.valueOf(id),
                "User status changed to " + request.getStatus(), ipAddress);
        return UserMapper.toDto(saved);
    }

    @Transactional
    public void deleteUser(Long id, Long actorId, String ipAddress) {
        User user = findUserOrThrow(id);
        // Enterprise-safe deletion: deactivate rather than hard-delete to preserve referential integrity/audit trail.
        user.setStatus(UserStatus.INACTIVE);
        userRepository.save(user);
        auditService.record(actorId, "USER_DELETE", "User", String.valueOf(id), "User deactivated (soft delete)", ipAddress);
    }

    private User findUserOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }
}
