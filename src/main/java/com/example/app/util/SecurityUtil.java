package com.example.app.util;

import com.example.app.exception.UnauthorizedException;
import com.example.app.security.UserPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtil {

    private SecurityUtil() {
    }

    public static UserPrincipal currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal)) {
            throw new UnauthorizedException("No authenticated user found");
        }
        return (UserPrincipal) auth.getPrincipal();
    }

    public static Long currentUserId() {
        return currentUser().getId();
    }

    public static String currentRole() {
        return currentUser().getRole();
    }

    public static boolean isAdmin() {
        return "ADMIN".equals(currentRole());
    }

    public static boolean isTeacher() {
        return "TEACHER".equals(currentRole());
    }

    public static boolean isStudent() {
        return "STUDENT".equals(currentRole());
    }
}
