package com.example.examseating.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SecurityConfigurationTest {

    @Test
    @DisplayName("JwtService: Missing secret must throw IllegalStateException during startup")
    void testJwtService_MissingSecretThrowsException() {
        JwtService jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", "");
        ReflectionTestUtils.setField(jwtService, "expiration", 86400000L);

        IllegalStateException ex = assertThrows(IllegalStateException.class, jwtService::validateConfiguration);
        assertTrue(ex.getMessage().contains("missing or empty"), "Expected error message about missing secret");
    }

    @Test
    @DisplayName("JwtService: Secret shorter than 32 bytes (256 bits) must throw IllegalStateException")
    void testJwtService_WeakSecretThrowsException() {
        JwtService jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", "short-16-bytes!!");
        ReflectionTestUtils.setField(jwtService, "expiration", 86400000L);

        IllegalStateException ex = assertThrows(IllegalStateException.class, jwtService::validateConfiguration);
        assertTrue(ex.getMessage().contains("too weak"), "Expected error message about weak secret");
    }

    @Test
    @DisplayName("JwtService: Valid 256-bit secret generates and verifies tokens successfully")
    void testJwtService_ValidSecretGeneratesToken() {
        String testSecret = "0123456789012345678901234567890123456789012345678901234567890123"; // 64 bytes
        JwtService jwtService = new JwtService(testSecret, 3600000L);
        jwtService.validateConfiguration();

        String token = jwtService.generateToken("admin", "ROLE_ADMIN");
        assertNotNull(token);
        assertTrue(jwtService.isTokenValid(token, "admin"));
        assertEquals("admin", jwtService.extractUsername(token));
        assertEquals("ROLE_ADMIN", jwtService.extractRole(token));
    }

    @Test
    @DisplayName("SecurityConfig: CORS restricts allowed origins to configured frontend domains, not wildcard")
    void testCorsConfiguration_RestrictsOrigins() {
        SecurityConfig config = new SecurityConfig();
        ReflectionTestUtils.setField(config, "allowedOrigins", "http://localhost:5173,https://my-app.vercel.app");

        CorsConfigurationSource source = config.corsConfigurationSource();
        assertNotNull(source);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/departments");
        request.addHeader("Origin", "http://localhost:5173");

        CorsConfiguration corsConfig = source.getCorsConfiguration(request);
        assertNotNull(corsConfig);

        List<String> allowed = corsConfig.getAllowedOrigins();
        assertNotNull(allowed);
        assertEquals(2, allowed.size());
        assertTrue(allowed.contains("http://localhost:5173"));
        assertTrue(allowed.contains("https://my-app.vercel.app"));
        assertFalse(allowed.contains("*"), "Wildcard origin must not be permitted");
        assertTrue(corsConfig.getAllowCredentials(), "AllowCredentials should be enabled for specific origin list");
    }
}
