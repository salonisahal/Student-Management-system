package com.example.app.service;

import com.example.app.audit.AuditService;
import com.example.app.dto.UserStatusUpdateRequest;
import com.example.app.entity.Role;
import com.example.app.entity.User;
import com.example.app.entity.UserStatus;
import com.example.app.exception.BadRequestException;
import com.example.app.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuditService auditService;

    @InjectMocks
    private UserService userService;

    @Test
    void preventsAdminFromDeactivatingOwnAccount() {
        UserStatusUpdateRequest request = new UserStatusUpdateRequest();
        request.setStatus(UserStatus.INACTIVE);

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> userService.updateStatus(1L, request, 1L, "127.0.0.1"));

        assertEquals("Administrators cannot deactivate or lock their own account", exception.getMessage());
        verify(userRepository, never()).findById(any());
    }

    @Test
    void preventsAdminFromLockingOwnAccount() {
        UserStatusUpdateRequest request = new UserStatusUpdateRequest();
        request.setStatus(UserStatus.LOCKED);

        assertThrows(BadRequestException.class,
                () -> userService.updateStatus(1L, request, 1L, "127.0.0.1"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void preventsAdminFromDeletingOwnAccount() {
        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> userService.deleteUser(1L, 1L, "127.0.0.1"));

        assertEquals("Administrators cannot delete or deactivate their own account", exception.getMessage());
        verify(userRepository, never()).findById(any());
    }

    @Test
    void permitsAdminToDeactivateAnotherUser() {
        UserStatusUpdateRequest request = new UserStatusUpdateRequest();
        request.setStatus(UserStatus.INACTIVE);
        User user = new User();
        user.setId(2L);
        user.setRole(Role.STUDENT);
        user.setStatus(UserStatus.ACTIVE);
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        assertDoesNotThrow(() -> userService.updateStatus(2L, request, 1L, "127.0.0.1"));

        assertEquals(UserStatus.INACTIVE, user.getStatus());
        verify(userRepository).save(user);
    }
}
