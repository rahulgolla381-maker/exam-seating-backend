package com.example.examseating.security;

import com.example.examseating.dto.LoginRequest;
import com.example.examseating.dto.LoginResponse;
import com.example.examseating.entity.Role;
import com.example.examseating.entity.User;
import com.example.examseating.repository.UserRepository;
import com.example.examseating.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticationTest {

    @Mock
    private UserRepository userRepository;

    @Spy
    private PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Spy
    private JwtService jwtService = new JwtService("42c3ffd710264edeb41da661651a7f0442c3ffd710264edeb41da661651a7f04", 86400000L);

    @InjectMocks
    private AuthService authService;

    private User adminUser;
    private User studentUser;

    @BeforeEach
    void setUp() {
        adminUser = new User();
        adminUser.setId(1L);
        adminUser.setUsername("admin");
        adminUser.setPassword(passwordEncoder.encode("Admin@123"));
        adminUser.setRole(Role.ADMIN);

        studentUser = new User();
        studentUser.setId(2L);
        studentUser.setUsername("student");
        studentUser.setPassword(passwordEncoder.encode("Student@123"));
        studentUser.setRole(Role.STUDENT);
    }

    @Test
    void test1_PasswordIsBCryptEncoded() {
        String rawPassword = "Admin@123";
        String encoded = passwordEncoder.encode(rawPassword);

        assertNotEquals(rawPassword, encoded);
        assertTrue(encoded.startsWith("$2a$") || encoded.startsWith("$2b$"));
        assertTrue(passwordEncoder.matches(rawPassword, encoded));
        assertFalse(passwordEncoder.matches("WrongPassword", encoded));
    }

    @Test
    void test2_ValidAdminLoginSucceeds() {
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));

        LoginRequest request = new LoginRequest("admin", "Admin@123");
        LoginResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("admin", response.getUsername());
        assertEquals("ADMIN", response.getRole());
        assertNotNull(response.getToken());
        assertFalse(response.getToken().isEmpty());
    }

    @Test
    void test3_ValidStudentLoginSucceeds() {
        when(userRepository.findByUsername("student")).thenReturn(Optional.of(studentUser));

        LoginRequest request = new LoginRequest("student", "Student@123");
        LoginResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("student", response.getUsername());
        assertEquals("STUDENT", response.getRole());
        assertNotNull(response.getToken());
    }

    @Test
    void test4_InvalidPasswordThrowsBadCredentials() {
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));

        LoginRequest request = new LoginRequest("admin", "WrongPassword@999");
        BadCredentialsException ex = assertThrows(BadCredentialsException.class, () -> authService.login(request));
        assertEquals("Invalid username or password.", ex.getMessage());
    }

    @Test
    void test5_UnknownUsernameThrowsBadCredentials() {
        when(userRepository.findByUsername("unknownUser")).thenReturn(Optional.empty());

        LoginRequest request = new LoginRequest("unknownUser", "Admin@123");
        BadCredentialsException ex = assertThrows(BadCredentialsException.class, () -> authService.login(request));
        assertEquals("Invalid username or password.", ex.getMessage());
    }

    @Test
    void test6_JwtTokenGenerationAndValidation() {
        String token = jwtService.generateToken("admin", "ADMIN");

        assertNotNull(token);
        assertEquals("admin", jwtService.extractUsername(token));
        assertEquals("ADMIN", jwtService.extractRole(token));
        assertTrue(jwtService.isTokenValid(token, "admin"));
        assertFalse(jwtService.isTokenValid(token, "otherUser"));
        assertFalse(jwtService.isTokenExpired(token));
    }

    @Test
    void test7_StudentRoleTokenValidation() {
        String token = jwtService.generateToken("student", "STUDENT");

        assertEquals("student", jwtService.extractUsername(token));
        assertEquals("STUDENT", jwtService.extractRole(token));
        assertTrue(jwtService.isTokenValid(token, "student"));
    }

    @Test
    void test8_ExpiredTokenIsDetectedAndInvalid() {
        JwtService expiredJwtService = new JwtService("42c3ffd710264edeb41da661651a7f0442c3ffd710264edeb41da661651a7f04", -1000L);
        String expiredToken = expiredJwtService.generateToken("admin", "ADMIN");

        assertTrue(expiredJwtService.isTokenExpired(expiredToken));
        assertFalse(expiredJwtService.isTokenValid(expiredToken, "admin"));
    }

    @Test
    void test9_TamperedTokenSignatureIsRejected() {
        String token = jwtService.generateToken("admin", "ADMIN");
        String[] parts = token.split("\\.");
        // Alter payload slightly
        String tamperedToken = parts[0] + "." + parts[1] + "tamper." + parts[2];

        assertFalse(jwtService.isTokenValid(tamperedToken, "admin"));
    }
}
