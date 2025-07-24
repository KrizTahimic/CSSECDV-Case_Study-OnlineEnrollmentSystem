package com.enrollment.auth.integration;

import com.enrollment.auth.dto.AuthRequest;
import com.enrollment.auth.dto.PasswordChangeRequest;
import com.enrollment.auth.dto.RegisterRequest;
import com.enrollment.auth.model.User;
import com.enrollment.auth.repository.UserRepository;
import com.enrollment.auth.service.AccountLockoutService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for security features in the Auth Service.
 * Tests complete security flows with real Spring context.
 * 
 * Security features tested:
 * - 2.1.4: Generic error messages
 * - 2.1.5, 2.1.6: Password complexity and length
 * - 2.1.8: Account lockout
 * - 2.1.10: Password history
 * - 2.1.11: Password age restriction
 * - 2.1.12: Last login tracking
 * - 2.1.13: Re-authentication
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AccountLockoutService lockoutService;

    @MockBean(name = "stringRedisTemplate")
    private RedisTemplate<String, String> redisTemplate;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        // Reset any lockouts
        lockoutService.resetFailedAttempts("test@example.com");
    }

    @Test
    @DisplayName("Should enforce password complexity requirements on registration")
    void shouldEnforcePasswordComplexityOnRegistration() throws Exception {
        // Test various invalid passwords
        String[] invalidPasswords = {
            "short",           // Too short
            "alllowercase",    // No uppercase, number, or special
            "ALLUPPERCASE",    // No lowercase, number, or special
            "NoNumbers!",      // No numbers
            "NoSpecial123",    // No special characters
            "Space 123A"       // Space is not a special character
        };

        for (String password : invalidPasswords) {
            RegisterRequest request = createRegisterRequest("test" + password.hashCode() + "@example.com", password);
            
            mockMvc.perform(post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Invalid input data provided"));
        }

        // Test valid password
        RegisterRequest validRequest = createRegisterRequest("valid@example.com", "Valid@123");
        
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists());
    }

    @Test
    @DisplayName("Should return generic error messages for authentication failures")
    void shouldReturnGenericErrorMessages() throws Exception {
        // Register a user first
        RegisterRequest registerRequest = createRegisterRequest("test@example.com", "Test@123");
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk());

        // Test non-existent user
        AuthRequest wrongUser = new AuthRequest();
        wrongUser.setEmail("nonexistent@example.com");
        wrongUser.setPassword("Test@123");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(wrongUser)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid username and/or password"));

        // Test wrong password
        AuthRequest wrongPassword = new AuthRequest();
        wrongPassword.setEmail("test@example.com");
        wrongPassword.setPassword("Wrong@123");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(wrongPassword)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid username and/or password"));
    }

    @Test
    @DisplayName("Should lock account after 5 failed login attempts")
    void shouldLockAccountAfterFiveFailedAttempts() throws Exception {
        // Register a user
        RegisterRequest registerRequest = createRegisterRequest("lockout@example.com", "Test@123");
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk());

        AuthRequest wrongPassword = new AuthRequest();
        wrongPassword.setEmail("lockout@example.com");
        wrongPassword.setPassword("Wrong@123");

        // Attempt login 5 times with wrong password
        for (int i = 1; i <= 5; i++) {
            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(wrongPassword)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Invalid username and/or password"));
        }

        // 6th attempt should still fail even with correct password
        AuthRequest correctPassword = new AuthRequest();
        correctPassword.setEmail("lockout@example.com");
        correctPassword.setPassword("Test@123");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(correctPassword)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid username and/or password"));
    }

    @Test
    @DisplayName("Should track last login information")
    void shouldTrackLastLoginInformation() throws Exception {
        // Register and login
        RegisterRequest registerRequest = createRegisterRequest("tracking@example.com", "Test@123");
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk());

        AuthRequest loginRequest = new AuthRequest();
        loginRequest.setEmail("tracking@example.com");
        loginRequest.setPassword("Test@123");

        // First login - no previous login info
        MvcResult firstLogin = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest))
                .header("X-Forwarded-For", "192.168.1.1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastLoginTime").doesNotExist())
                .andReturn();

        // Wait a moment
        Thread.sleep(100);

        // Second login - should show previous login info
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest))
                .header("X-Forwarded-For", "192.168.1.2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastLoginTime").exists())
                .andExpect(jsonPath("$.lastLoginIP").value("192.168.1.1"));
    }

    @Test
    @DisplayName("Should prevent password reuse from history")
    void shouldPreventPasswordReuseFromHistory() throws Exception {
        // Create user with password history
        User user = User.builder()
                .email("history@example.com")
                .password(passwordEncoder.encode("Current@123"))
                .firstName("Test")
                .lastName("User")
                .role("student")
                .passwordHistory(new ArrayList<>(Arrays.asList(
                    passwordEncoder.encode("Old@Pass1"),
                    passwordEncoder.encode("Old@Pass2"),
                    passwordEncoder.encode("Old@Pass3"),
                    passwordEncoder.encode("Old@Pass4"),
                    passwordEncoder.encode("Current@123")
                )))
                .passwordChangedAt(LocalDateTime.now().minusDays(2))
                .build();
        userRepository.save(user);

        // Login to get token
        String token = loginAndGetToken("history@example.com", "Current@123");

        // Try to change to a password from history
        PasswordChangeRequest changeRequest = new PasswordChangeRequest();
        changeRequest.setCurrentPassword("Current@123");
        changeRequest.setNewPassword("Old@Pass2"); // From history

        mockMvc.perform(post("/api/auth/change-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(changeRequest))
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Password has been used recently. Please choose a different password"));

        // Try with a new password not in history
        changeRequest.setNewPassword("NewPass@456");
        
        mockMvc.perform(post("/api/auth/change-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(changeRequest))
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password changed successfully"));
    }

    @Test
    @DisplayName("Should enforce 24-hour password age restriction")
    void shouldEnforce24HourPasswordAgeRestriction() throws Exception {
        // Create user with recent password change
        User user = User.builder()
                .email("age@example.com")
                .password(passwordEncoder.encode("Current@123"))
                .firstName("Test")
                .lastName("User")
                .role("student")
                .passwordChangedAt(LocalDateTime.now().minusHours(12)) // Changed 12 hours ago
                .passwordHistory(new ArrayList<>())
                .build();
        userRepository.save(user);

        String token = loginAndGetToken("age@example.com", "Current@123");

        PasswordChangeRequest changeRequest = new PasswordChangeRequest();
        changeRequest.setCurrentPassword("Current@123");
        changeRequest.setNewPassword("NewPass@456");

        // Should fail - password too new
        mockMvc.perform(post("/api/auth/change-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(changeRequest))
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Password must be at least 24 hours old before it can be changed"));

        // Update password age to be older than 24 hours
        user.setPasswordChangedAt(LocalDateTime.now().minusDays(2));
        userRepository.save(user);

        // Should succeed now
        mockMvc.perform(post("/api/auth/change-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(changeRequest))
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password changed successfully"));
    }

    @Test
    @DisplayName("Should require re-authentication for sensitive operations")
    void shouldRequireReauthenticationForSensitiveOperations() throws Exception {
        // Register user
        RegisterRequest registerRequest = createRegisterRequest("reauth@example.com", "Test@123");
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk());

        String token = loginAndGetToken("reauth@example.com", "Test@123");

        // Test reauthentication endpoint with correct password
        AuthRequest reauth = new AuthRequest();
        reauth.setEmail("reauth@example.com");
        reauth.setPassword("Test@123");

        mockMvc.perform(post("/api/auth/reauthenticate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reauth))
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Authentication successful"));

        // Test with wrong password
        reauth.setPassword("Wrong@123");

        mockMvc.perform(post("/api/auth/reauthenticate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reauth))
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid credentials"));

        // Test with mismatched email
        reauth.setEmail("different@example.com");
        reauth.setPassword("Test@123");

        mockMvc.perform(post("/api/auth/reauthenticate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reauth))
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid credentials"));
    }

    @Test
    @DisplayName("Should validate security questions on registration")
    void shouldValidateSecurityQuestionsOnRegistration() throws Exception {
        // Test with invalid security question
        RegisterRequest request = createRegisterRequest("security@example.com", "Test@123");
        request.setSecurityQuestion("Invalid question");
        request.setSecurityAnswer("Answer");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid security question"));

        // Test with valid security question
        request.setSecurityQuestion("What is your favorite color?");
        
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // Verify security answer is hashed
        User savedUser = userRepository.findByEmail("security@example.com").orElseThrow();
        assertNotEquals("Answer", savedUser.getSecurityAnswer());
        assertTrue(passwordEncoder.matches("Answer", savedUser.getSecurityAnswer()));
    }

    // Helper methods
    private RegisterRequest createRegisterRequest(String email, String password) {
        RegisterRequest request = new RegisterRequest();
        request.setEmail(email);
        request.setPassword(password);
        request.setFirstName("Test");
        request.setLastName("User");
        request.setRole("student");
        return request;
    }

    private String loginAndGetToken(String email, String password) throws Exception {
        AuthRequest loginRequest = new AuthRequest();
        loginRequest.setEmail(email);
        loginRequest.setPassword(password);

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        return objectMapper.readTree(responseBody).get("token").asText();
    }
}