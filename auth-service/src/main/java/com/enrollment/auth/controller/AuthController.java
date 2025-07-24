package com.enrollment.auth.controller;

import com.enrollment.auth.dto.AuthRequest;
import com.enrollment.auth.dto.AuthResponse;
import com.enrollment.auth.dto.RegisterRequest;
import com.enrollment.auth.dto.PasswordChangeRequest;
import com.enrollment.auth.model.User;
import com.enrollment.auth.repository.UserRepository;
import com.enrollment.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import jakarta.validation.Valid;

import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;
    
    @Value("${jwt.secret}")
    private String secret;

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Received registration request for email: {}", request.getEmail());
        try {
            AuthResponse response = authService.register(request);
            log.info("Successfully registered user: {}", request.getEmail());
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("Registration failed for email: {}, error: {}", request.getEmail(), e.getMessage(), e);
            Map<String, String> response = new HashMap<>();
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            log.error("Unexpected error during registration for email: {}", request.getEmail(), e);
            Map<String, String> response = new HashMap<>();
            response.put("message", "An unexpected error occurred");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest request, HttpServletRequest servletRequest) {
        log.info("Received login request for email: {}", request.getEmail());
        try {
            if (request.getEmail() == null || request.getEmail().isEmpty() || 
                request.getPassword() == null || request.getPassword().isEmpty()) {
                log.error("Missing email or password");
                return ResponseEntity.badRequest().body(Map.of("message", "Invalid username and/or password"));
            }

            // Get client IP address
            String ipAddress = getClientIP(servletRequest);
            request.setIpAddress(ipAddress);

            AuthResponse response = authService.login(request);
            log.info("Successfully logged in user: {}", request.getEmail());
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("Login failed for email: {}, error: {}", request.getEmail(), e.getMessage(), e);
            Map<String, String> response = new HashMap<>();
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            log.error("Unexpected error during login for email: {}", request.getEmail(), e);
            Map<String, String> response = new HashMap<>();
            response.put("message", "An unexpected error occurred");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@RequestHeader("Authorization") String token) {
        try {
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
                
                // Parse the token to get the claims
                byte[] keyBytes = Decoders.BASE64.decode(secret);
                var key = Keys.hmacShaKeyFor(keyBytes);
                
                Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
                
                String userId = claims.get("id", String.class);
                String email = claims.getSubject();
                
                Optional<User> userOpt;
                if (userId != null) {
                    userOpt = userRepository.findById(userId);
                } else {
                    userOpt = userRepository.findByEmail(email);
                }
                
                if (userOpt.isPresent()) {
                    return ResponseEntity.ok(new UserDTO(userOpt.get()));
                }
            }
            
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Invalid or expired token"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Error processing token: " + e.getMessage()));
        }
    }
    
    private static class UserDTO {
        private final String id;
        private final String firstName;
        private final String lastName;
        private final String email;
        private final String role;
        
        public UserDTO(User user) {
            this.id = user.getId();
            this.firstName = user.getFirstName();
            this.lastName = user.getLastName();
            this.email = user.getEmail();
            this.role = user.getRole();
        }
        
        public String getId() { return id; }
        public String getFirstName() { return firstName; }
        public String getLastName() { return lastName; }
        public String getEmail() { return email; }
        public String getRole() { return role; }
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<?> getUserById(@PathVariable String id) {
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isPresent()) {
            return ResponseEntity.ok(new UserDTO(userOpt.get()));
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", "User not found"));
    }
    
    @GetMapping("/users")
    public ResponseEntity<?> getUsersByRole(@RequestParam(required = false) String role) {
        List<User> users;
        if (role != null && !role.isEmpty()) {
            users = userRepository.findByRole(role);
        } else {
            users = userRepository.findAll();
        }
        
        List<UserDTO> userDTOs = users.stream()
                .map(UserDTO::new)
                .collect(Collectors.toList());
                
        return ResponseEntity.ok(userDTOs);
    }

    @GetMapping("/users/email/{email}")
    public ResponseEntity<?> getUserByEmail(@PathVariable String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isPresent()) {
            return ResponseEntity.ok(new UserDTO(userOpt.get()));
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", "User not found"));
    }
    
    /**
     * Changes user password with security validations.
     * Implements requirements:
     * - 2.1.10: Prevent password re-use (checks last 5 passwords)
     * - 2.1.11: Password must be at least 1 day old before change
     * - 2.1.13: Requires re-authentication
     */
    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@Valid @RequestBody PasswordChangeRequest request,
                                          @RequestHeader("Authorization") String token) {
        try {
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
                
                // Parse token to get user email
                byte[] keyBytes = Decoders.BASE64.decode(secret);
                var key = Keys.hmacShaKeyFor(keyBytes);
                
                Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
                
                String email = claims.getSubject();
                
                // Change password
                authService.changePassword(email, request);
                
                return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
            }
            
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Invalid or expired token"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("Error changing password", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error changing password"));
        }
    }
    
    /**
     * Re-authenticates user for sensitive operations.
     * Implements requirement 2.1.13.
     */
    @PostMapping("/reauthenticate")
    public ResponseEntity<?> reauthenticate(@RequestBody AuthRequest request,
                                          @RequestHeader("Authorization") String token) {
        try {
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
                
                // Parse token to get user email
                byte[] keyBytes = Decoders.BASE64.decode(secret);
                var key = Keys.hmacShaKeyFor(keyBytes);
                
                Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
                
                String email = claims.getSubject();
                
                // Verify email matches
                if (!email.equals(request.getEmail())) {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                            .body(Map.of("message", "Invalid credentials"));
                }
                
                // Verify password
                boolean isValid = authService.verifyPassword(email, request.getPassword());
                if (isValid) {
                    return ResponseEntity.ok(Map.of("message", "Authentication successful"));
                } else {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                            .body(Map.of("message", "Invalid credentials"));
                }
            }
            
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Invalid or expired token"));
        } catch (Exception e) {
            log.error("Error during reauthentication", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Authentication failed"));
        }
    }
    
    private String getClientIP(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIP = request.getHeader("X-Real-IP");
        if (xRealIP != null && !xRealIP.isEmpty()) {
            return xRealIP;
        }
        
        return request.getRemoteAddr();
    }
} 