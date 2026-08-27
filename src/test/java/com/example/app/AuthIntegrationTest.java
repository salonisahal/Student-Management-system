package com.example.app;

import com.example.app.entity.Role;
import com.example.app.entity.User;
import com.example.app.entity.UserStatus;
import com.example.app.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        User admin = new User();
        admin.setFirstName("Admin");
        admin.setLastName("User");
        admin.setEmail("admin-it@example.com");
        admin.setPassword(passwordEncoder.encode("Admin@123"));
        admin.setRole(Role.ADMIN);
        admin.setStatus(UserStatus.ACTIVE);
        admin.setCreatedAt(LocalDateTime.now());
        admin.setUpdatedAt(LocalDateTime.now());
        userRepository.save(admin);
    }

    @Test
    void healthEndpointIsPublic() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void loginWithValidCredentialsReturnsTokens() throws Exception {
        Map<String, String> body = new HashMap<>();
        body.put("email", "admin-it@example.com");
        body.put("password", "Admin@123");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andExpect(jsonPath("$.data.refreshToken").exists());
    }

    @Test
    void loginWithInvalidCredentialsReturns401() throws Exception {
        Map<String, String> body = new HashMap<>();
        body.put("email", "admin-it@example.com");
        body.put("password", "wrong-password");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpointWithoutTokenIsUnauthorized() throws Exception {
        // No credentials supplied at all -> caller was never authenticated,
        // so this must be 401 ("please log in"), not 403 ("not allowed").
        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpointWithMalformedTokenIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/users").header("Authorization", "Bearer not-a-real-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpointWithExpiredTokenIsUnauthorized() throws Exception {
        // An expired/garbage-signature JWT-shaped token should also be 401, not 403.
        String expiredLookingToken = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbi1pdEBleGFtcGxlLmNvbSIsImV4cCI6MTB9.invalidsignature";
        mockMvc.perform(get("/api/v1/users").header("Authorization", "Bearer " + expiredLookingToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void studentRoleCannotAccessAdminEndpointGetsForbiddenNotUnauthorized() throws Exception {
        // A genuinely authenticated user who simply lacks the required role
        // must still get 403, to distinguish it from "not logged in" (401).
        User student = new User();
        student.setFirstName("Stu");
        student.setLastName("Dent");
        student.setEmail("student-it@example.com");
        student.setPassword(passwordEncoder.encode("Student@123"));
        student.setRole(Role.STUDENT);
        student.setStatus(UserStatus.ACTIVE);
        student.setCreatedAt(LocalDateTime.now());
        student.setUpdatedAt(LocalDateTime.now());
        userRepository.save(student);

        Map<String, String> body = new HashMap<>();
        body.put("email", "student-it@example.com");
        body.put("password", "Student@123");

        String response = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String token = objectMapper.readTree(response).get("data").get("accessToken").asText();

        mockMvc.perform(get("/api/v1/users").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanAccessUserManagementAfterLogin() throws Exception {
        Map<String, String> body = new HashMap<>();
        body.put("email", "admin-it@example.com");
        body.put("password", "Admin@123");

        String response = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String token = objectMapper.readTree(response).get("data").get("accessToken").asText();

        mockMvc.perform(get("/api/v1/users").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }
}
