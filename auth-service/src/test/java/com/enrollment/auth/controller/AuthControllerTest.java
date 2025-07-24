package com.enrollment.auth.controller;

import com.enrollment.auth.dto.AuthRequest;
import com.enrollment.auth.dto.AuthResponse;
import com.enrollment.auth.dto.PasswordChangeRequest;
import com.enrollment.auth.dto.RegisterRequest;
import com.enrollment.auth.model.User;
import com.enrollment.auth.repository.UserRepository;
import com.enrollment.auth.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests for AuthController endpoints.
 * Focuses on testing API behavior, request/response handling, and error scenarios.
 */
@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private UserRepository userRepository;

    private RegisterRequest validRegisterRequest;
    private AuthRequest validAuthRequest;
    private AuthResponse authResponse;

    @BeforeEach
    void setUp() {
        // Setup valid test data
        validRegisterRequest = new RegisterRequest();
        validRegisterRequest.setEmail("test@example.com");
        validRegisterRequest.setPassword("Test@123");
        validRegisterRequest.setFirstName("Test");
        validRegisterRequest.setLastName("User");
        validRegisterRequest.setRole("student");
        validRegisterRequest.setSecurityQuestion("What is your favorite color?");
        validRegisterRequest.setSecurityAnswer("Blue");

        validAuthRequest = new AuthRequest();
        validAuthRequest.setEmail("test@example.com");
        validAuthRequest.setPassword("Test@123");

        authResponse = AuthResponse.builder()
                .token("jwt.token.here")
                .email("test@example.com")
                .firstName("Test")
                .lastName("User")
                .role("student")
                .build();
    }

    @Test
    @DisplayName("Should register user successfully")
    void shouldRegisterUserSuccessfully() throws Exception {
        when(authService.register(any(RegisterRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRegisterRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt.token.here"))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.firstName").value("Test"))
                .andExpect(jsonPath("$.lastName").value("User"))
                .andExpect(jsonPath("$.role").value("student"));

        verify(authService).register(any(RegisterRequest.class));
    }

    @Test
    @DisplayName("Should reject registration with invalid password")
    void shouldRejectRegistrationWithInvalidPassword() throws Exception {
        RegisterRequest invalidRequest = new RegisterRequest();
        invalidRequest.setEmail("test@example.com");
        invalidRequest.setPassword("weak"); // Invalid password
        invalidRequest.setFirstName("Test");
        invalidRequest.setLastName("User");
        invalidRequest.setRole("student");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid input data provided"));
    }

    @Test
    @DisplayName("Should handle registration service exceptions")
    void shouldHandleRegistrationServiceExceptions() throws Exception {
        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new RuntimeException("User already exists"));

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRegisterRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("User already exists"));
    }

    @Test
    @DisplayName("Should login successfully with valid credentials")
    void shouldLoginSuccessfully() throws Exception {
        when(authService.login(any(AuthRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validAuthRequest))
                .header("X-Forwarded-For", "192.168.1.1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt.token.here"))
                .andExpect(jsonPath("$.email").value("test@example.com"));

        verify(authService).login(argThat(request -> 
            request.getIpAddress() != null && request.getIpAddress().equals("192.168.1.1")
        ));
    }

    @Test
    @DisplayName("Should return generic error for invalid credentials")
    void shouldReturnGenericErrorForInvalidCredentials() throws Exception {
        when(authService.login(any(AuthRequest.class)))
                .thenThrow(new RuntimeException("Invalid username and/or password"));

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validAuthRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid username and/or password"));
    }

    @Test
    @DisplayName("Should reject login with missing credentials")
    void shouldRejectLoginWithMissingCredentials() throws Exception {
        AuthRequest emptyRequest = new AuthRequest();
        emptyRequest.setEmail("");
        emptyRequest.setPassword("");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(emptyRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid username and/or password"));

        verify(authService, never()).login(any());
    }

    @Test
    @DisplayName("Should extract client IP from headers")
    void shouldExtractClientIPFromHeaders() throws Exception {
        when(authService.login(any(AuthRequest.class))).thenReturn(authResponse);

        // Test X-Forwarded-For
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validAuthRequest))
                .header("X-Forwarded-For", "10.0.0.1, 192.168.1.1"))
                .andExpect(status().isOk());

        verify(authService).login(argThat(request -> 
            request.getIpAddress().equals("10.0.0.1")
        ));

        // Test X-Real-IP
        reset(authService);
        when(authService.login(any(AuthRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validAuthRequest))
                .header("X-Real-IP", "172.16.0.1"))
                .andExpect(status().isOk());

        verify(authService).login(argThat(request -> 
            request.getIpAddress().equals("172.16.0.1")
        ));
    }

    @Test
    @DisplayName("Should change password successfully")
    void shouldChangePasswordSuccessfully() throws Exception {
        PasswordChangeRequest changeRequest = new PasswordChangeRequest();
        changeRequest.setCurrentPassword("OldPass@123");
        changeRequest.setNewPassword("NewPass@456");

        String validToken = "Bearer valid.jwt.token";

        mockMvc.perform(post("/api/auth/change-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(changeRequest))
                .header("Authorization", validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password changed successfully"));
    }

    @Test
    @DisplayName("Should reject password change without authorization")
    void shouldRejectPasswordChangeWithoutAuthorization() throws Exception {
        PasswordChangeRequest changeRequest = new PasswordChangeRequest();
        changeRequest.setCurrentPassword("OldPass@123");
        changeRequest.setNewPassword("NewPass@456");

        mockMvc.perform(post("/api/auth/change-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(changeRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should validate password complexity on change")
    void shouldValidatePasswordComplexityOnChange() throws Exception {
        PasswordChangeRequest changeRequest = new PasswordChangeRequest();
        changeRequest.setCurrentPassword("OldPass@123");
        changeRequest.setNewPassword("weak"); // Invalid new password

        String validToken = "Bearer valid.jwt.token";

        mockMvc.perform(post("/api/auth/change-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(changeRequest))
                .header("Authorization", validToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid input data provided"));
    }

    @Test
    @DisplayName("Should reauthenticate successfully")
    void shouldReauthenticateSuccessfully() throws Exception {
        when(authService.verifyPassword(anyString(), anyString())).thenReturn(true);

        String validToken = "Bearer valid.jwt.token";

        mockMvc.perform(post("/api/auth/reauthenticate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validAuthRequest))
                .header("Authorization", validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Authentication successful"));
    }

    @Test
    @DisplayName("Should reject reauthentication with invalid credentials")
    void shouldRejectReauthenticationWithInvalidCredentials() throws Exception {
        when(authService.verifyPassword(anyString(), anyString())).thenReturn(false);

        String validToken = "Bearer valid.jwt.token";

        mockMvc.perform(post("/api/auth/reauthenticate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validAuthRequest))
                .header("Authorization", validToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid credentials"));
    }

    @Test
    @DisplayName("Should get current user successfully")
    void shouldGetCurrentUserSuccessfully() throws Exception {
        User user = User.builder()
                .id("user123")
                .email("test@example.com")
                .firstName("Test")
                .lastName("User")
                .role("student")
                .build();

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        String validToken = "Bearer valid.jwt.token";

        mockMvc.perform(get("/api/auth/me")
                .header("Authorization", validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.firstName").value("Test"))
                .andExpect(jsonPath("$.lastName").value("User"))
                .andExpect(jsonPath("$.role").value("student"));
    }

    @Test
    @DisplayName("Should return 401 for invalid token")
    void shouldReturn401ForInvalidToken() throws Exception {
        mockMvc.perform(get("/api/auth/me")
                .header("Authorization", "Bearer invalid.token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("Should handle missing authorization header")
    void shouldHandleMissingAuthorizationHeader() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid or expired token"));
    }

    @Test
    @DisplayName("Should include last login info in login response")
    void shouldIncludeLastLoginInfoInResponse() throws Exception {
        AuthResponse responseWithLastLogin = AuthResponse.builder()
                .token("jwt.token.here")
                .email("test@example.com")
                .firstName("Test")
                .lastName("User")
                .role("student")
                .build();
        responseWithLastLogin.setLastLoginTime("2024-01-15T10:30:00");
        responseWithLastLogin.setLastLoginIP("192.168.1.1");

        when(authService.login(any(AuthRequest.class))).thenReturn(responseWithLastLogin);

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validAuthRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastLoginTime").value("2024-01-15T10:30:00"))
                .andExpect(jsonPath("$.lastLoginIP").value("192.168.1.1"));
    }
}