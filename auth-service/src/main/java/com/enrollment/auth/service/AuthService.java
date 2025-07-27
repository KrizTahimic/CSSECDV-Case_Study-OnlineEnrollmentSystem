package com.enrollment.auth.service;

import com.enrollment.auth.dto.AuthRequest;
import com.enrollment.auth.dto.AuthResponse;
import com.enrollment.auth.dto.RegisterRequest;
import com.enrollment.auth.dto.PasswordChangeRequest;
import com.enrollment.auth.model.User;
import com.enrollment.auth.repository.UserRepository;
import com.enrollment.auth.config.SecurityQuestions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.io.Decoders;
import org.springframework.beans.factory.annotation.Value;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;

/**
 * Main authentication service implementing security requirements.
 * 
 * Security features implemented:
 * - Password complexity validation (2.1.5, 2.1.6)
 * - Security questions for password reset (2.1.9)
 * - Account lockout mechanism (2.1.8)
 * - Password history tracking (2.1.10)
 * - Last login tracking (2.1.12)
 * - Generic error messages (2.1.4)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccountLockoutService lockoutService;
    private final SecurityEventLogger securityLogger;
    
    @Value("${jwt.secret}")
    private String secret;
    
    @Value("${jwt.expiration}")
    private long expiration;

    /**
     * Registers a new user with security validations.
     * 
     * Features:
     * - Password complexity validation via @ValidPassword
     * - Security question setup for password reset
     * - Password history initialization
     * - Secure password and answer hashing
     * 
     * @param request Registration details including security question
     * @return AuthResponse with JWT token
     * @throws RuntimeException for validation failures
     */
    public AuthResponse register(RegisterRequest request) {
        log.info("Starting registration process for email: {}", request.getEmail());
        
        try {
            // Check if user already exists
            log.info("Checking if user exists with email: {}", request.getEmail());
            if (userRepository.findByEmail(request.getEmail()).isPresent()) {
                log.error("User with email {} already exists", request.getEmail());
                throw new RuntimeException("User already exists");
            }

            // Validate role
            log.info("Validating role: {}", request.getRole());
            List<String> validRoles = Arrays.asList("student", "instructor", "faculty", "Faculty", "admin");
            String role = request.getRole();
            if (!validRoles.contains(role)) {
                log.error("Invalid role: {}", request.getRole());
                throw new RuntimeException("Invalid role. Must be one of: " + validRoles);
            }
            
            // Validate security question if provided
            if (request.getSecurityQuestion() != null && !request.getSecurityQuestion().isEmpty()) {
                if (!SecurityQuestions.isValidQuestion(request.getSecurityQuestion())) {
                    log.error("Invalid security question provided");
                    throw new RuntimeException("Invalid security question");
                }
                if (request.getSecurityAnswer() == null || request.getSecurityAnswer().isEmpty()) {
                    log.error("Security answer is required when security question is provided");
                    throw new RuntimeException("Security answer is required");
                }
            }

            // Create new user
            log.info("Creating new user with email: {}", request.getEmail());
            String encodedPassword = passwordEncoder.encode(request.getPassword());
            
            // Initialize password history with the first password
            List<String> passwordHistory = new ArrayList<>();
            passwordHistory.add(encodedPassword);
            
            User user = User.builder()
                .email(request.getEmail())
                .password(encodedPassword)
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .role(role)
                .securityQuestion(request.getSecurityQuestion())
                .securityAnswer(request.getSecurityAnswer() != null ? 
                    passwordEncoder.encode(request.getSecurityAnswer()) : null)
                .passwordHistory(passwordHistory)
                .passwordChangedAt(LocalDateTime.now())
                .failedLoginAttempts(0)
                .build();

            // Save user
            log.info("Saving user to database");
            user = userRepository.save(user);
            log.info("User saved successfully with ID: {}", user.getId());

            // Generate token
            log.info("Generating JWT token");
            String token = generateToken(user);
            log.info("Token generated successfully for user: {}", user.getEmail());

            return AuthResponse.builder()
                .token(token)
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole())
                .build();
        } catch (Exception e) {
            log.error("Error during registration for email {}: {}", request.getEmail(), e.getMessage(), e);
            throw new RuntimeException("Error during registration: " + e.getMessage());
        }
    }

    /**
     * Authenticates a user with security checks.
     * 
     * Security features:
     * - Account lockout check (5 attempts, 15 min lockout)
     * - Failed attempt tracking
     * - Last login time/IP tracking
     * - Generic error messages for all failures
     * 
     * @param request Login credentials with IP address
     * @return AuthResponse with JWT token and last login info
     * @throws RuntimeException with generic message for all failures
     */
    public AuthResponse login(AuthRequest request) {
        log.info("Starting login process for email: {}", request.getEmail());
        
        try {
            // Check if account is locked (requirement 2.1.8)
            if (lockoutService.isAccountLocked(request.getEmail())) {
                log.warn("Login attempt for locked account: {}", request.getEmail());
                securityLogger.logAuthenticationFailure(request.getEmail(), 
                    request.getIpAddress(), "Account locked");
                throw new RuntimeException("Invalid username and/or password");
            }
            
            // Find user
            User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    log.error("User not found with email: {}", request.getEmail());
                    lockoutService.recordFailedAttempt(request.getEmail());
                    securityLogger.logAuthenticationFailure(request.getEmail(), 
                        request.getIpAddress(), "User not found");
                    return new RuntimeException("Invalid username and/or password");
                });

            // Verify password
            if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                log.error("Invalid password for user: {}", request.getEmail());
                lockoutService.recordFailedAttempt(request.getEmail());
                securityLogger.logAuthenticationFailure(request.getEmail(), 
                    request.getIpAddress(), "Invalid password");
                
                // Log lockout if this was the 5th attempt
                if (lockoutService.getFailedAttempts(request.getEmail()) >= 5) {
                    securityLogger.logAccountLockout(request.getEmail(), 5);
                }
                
                throw new RuntimeException("Invalid username and/or password");
            }
            
            // Reset failed attempts on successful login
            lockoutService.resetFailedAttempts(request.getEmail());
            
            // Update last login information (requirement 2.1.12)
            user.setPreviousLoginTime(user.getLastLoginTime());
            user.setPreviousLoginIP(user.getLastLoginIP());
            user.setLastLoginTime(LocalDateTime.now());
            user.setLastLoginIP(request.getIpAddress() != null ? request.getIpAddress() : "Unknown");
            userRepository.save(user);

            // Generate token
            String token = generateToken(user);
            log.info("Login successful for user: {}", request.getEmail());
            
            // Log successful authentication (requirement 2.4.6)
            securityLogger.logAuthenticationSuccess(request.getEmail(), request.getIpAddress());

            // Build response with last login info
            AuthResponse response = AuthResponse.builder()
                .token(token)
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole())
                .build();
                
            // Add last login info to response (requirement 2.1.12)
            if (user.getPreviousLoginTime() != null) {
                response.setLastLoginTime(user.getPreviousLoginTime().toString());
                response.setLastLoginIP(user.getPreviousLoginIP());
            }
            
            return response;
        } catch (RuntimeException e) {
            log.error("Login failed for email {}: {}", request.getEmail(), e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during login for email {}: {}", request.getEmail(), e.getMessage(), e);
            throw new RuntimeException("Error during login: " + e.getMessage());
        }
    }

    private String generateToken(User user) {
        log.debug("Generating token for user: {}", user.getEmail());
        try {
            byte[] keyBytes = Decoders.BASE64.decode(secret);
            var key = Keys.hmacShaKeyFor(keyBytes);
            
            Map<String, Object> claims = new HashMap<>();
            claims.put("roles", List.of(user.getRole().toUpperCase()));
            
            return Jwts.builder()
                .setClaims(claims)
                .setSubject(user.getEmail())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
        } catch (Exception e) {
            log.error("Error generating token for user {}: {}", user.getEmail(), e.getMessage(), e);
            throw new RuntimeException("Error generating token: " + e.getMessage());
        }
    }
    
    /**
     * Changes user password with security validations.
     * 
     * Validations:
     * - Current password must be correct
     * - New password must meet complexity requirements
     * - Password must not be in history (last 5)
     * - Password must be at least 24 hours old
     * 
     * @param email User email
     * @param request Password change request
     * @throws RuntimeException for validation failures
     */
    public void changePassword(String email, PasswordChangeRequest request) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Verify current password
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            log.error("Invalid current password for password change attempt: {}", email);
            securityLogger.logPasswordChangeFailure(email, "Invalid current password");
            throw new RuntimeException("Invalid current password");
        }
        
        // Check password age (requirement 2.1.11)
        if (user.getPasswordChangedAt() != null) {
            LocalDateTime minChangeTime = user.getPasswordChangedAt().plusDays(1);
            if (LocalDateTime.now().isBefore(minChangeTime)) {
                log.warn("Password change attempted too soon for user: {}", email);
                securityLogger.logPasswordChangeFailure(email, "Password too new");
                throw new RuntimeException("Password must be at least 24 hours old before it can be changed");
            }
        }
        
        // Check password history (requirement 2.1.10)
        String encodedNewPassword = passwordEncoder.encode(request.getNewPassword());
        if (user.getPasswordHistory() != null) {
            for (String oldPassword : user.getPasswordHistory()) {
                if (passwordEncoder.matches(request.getNewPassword(), oldPassword)) {
                    log.warn("Password reuse attempted for user: {}", email);
                    securityLogger.logPasswordChangeFailure(email, "Password reuse");
                    throw new RuntimeException("Password has been used recently. Please choose a different password");
                }
            }
        }
        
        // Update password and history
        user.setPassword(encodedNewPassword);
        
        // Maintain password history (keep last 5)
        if (user.getPasswordHistory() == null) {
            user.setPasswordHistory(new ArrayList<>());
        }
        user.getPasswordHistory().add(encodedNewPassword);
        if (user.getPasswordHistory().size() > 5) {
            user.getPasswordHistory().remove(0);
        }
        
        user.setPasswordChangedAt(LocalDateTime.now());
        userRepository.save(user);
        
        log.info("Password changed successfully for user: {}", email);
        securityLogger.logPasswordChange(email);
    }
    
    /**
     * Verifies user password for re-authentication.
     * 
     * @param email User email
     * @param password Password to verify
     * @return true if password is valid
     */
    public boolean verifyPassword(String email, String password) {
        return userRepository.findByEmail(email)
            .map(user -> passwordEncoder.matches(password, user.getPassword()))
            .orElse(false);
    }
} 