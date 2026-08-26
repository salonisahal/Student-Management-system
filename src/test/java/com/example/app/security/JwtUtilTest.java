package com.example.app.security;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil("dGhpc2lzYXZlcnlsb25nc2VjcmV0a2V5Zm9yc3R1ZGVudG1hbmFnZW1lbnRzeXN0ZW1qd3RzaWduaW5nMjAyNA==", 1800000);
    }

    @Test
    void generatesValidTokenWithExpectedClaims() {
        String token = jwtUtil.generateAccessToken(42L, "user@example.com", "ADMIN");
        assertNotNull(token);
        assertTrue(jwtUtil.isTokenValid(token));
        assertEquals("user@example.com", jwtUtil.getEmailFromToken(token));
        assertEquals(42L, jwtUtil.getUserIdFromToken(token));
        assertEquals("ADMIN", jwtUtil.getRoleFromToken(token));
    }

    @Test
    void rejectsTamperedToken() {
        String token = jwtUtil.generateAccessToken(1L, "a@b.com", "STUDENT");
        String tampered = token.substring(0, token.length() - 2) + "xx";
        assertFalse(jwtUtil.isTokenValid(tampered));
    }

    @Test
    void expiredTokenIsInvalid() throws InterruptedException {
        JwtUtil shortLived = new JwtUtil("dGhpc2lzYXZlcnlsb25nc2VjcmV0a2V5Zm9yc3R1ZGVudG1hbmFnZW1lbnRzeXN0ZW1qd3RzaWduaW5nMjAyNA==", 1);
        String token = shortLived.generateAccessToken(1L, "a@b.com", "STUDENT");
        Thread.sleep(50);
        assertFalse(shortLived.isTokenValid(token));
    }
}
