package com.enrollment.auth.controller;

import com.enrollment.auth.dto.AuthRequest;
import com.enrollment.auth.dto.AuthResponse;
import com.enrollment.auth.dto.RegisterRequest;
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

import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        log.info("Received registration request for email: {}", request.getEmail());
        try {
            if (request.getEmail() == null || request.getEmail().isEmpty()) {
                log.error("Email is required");
                return ResponseEntity.badRequest().body(Map.of("message", "Email is required"));
            }
            if (request.getPassword() == null || request.getPassword().isEmpty()) {
                log.error("Password is required");
                return ResponseEntity.badRequest().body(Map.of("message", "Password is required"));
            }
            if (request.getFirstName() == null || request.getFirstName().isEmpty()) {
                log.error("First name is required");
                return ResponseEntity.badRequest().body(Map.of("message", "First name is required"));
            }
            if (request.getLastName() == null || request.getLastName().isEmpty()) {
                log.error("Last name is required");
                return ResponseEntity.badRequest().body(Map.of("message", "Last name is required"));
            }
            if (request.getRole() == null || request.getRole().isEmpty()) {
                log.error("Role is required");
                return ResponseEntity.badRequest().body(Map.of("message", "Role is required"));
            }

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
    public ResponseEntity<?> login(@RequestBody AuthRequest request) {
        log.info("Received login request for email: {}", request.getEmail());
        try {
            if (request.getEmail() == null || request.getEmail().isEmpty()) {
                log.error("Email is required");
                return ResponseEntity.badRequest().body(Map.of("message", "Email is required"));
            }
            if (request.getPassword() == null || request.getPassword().isEmpty()) {
                log.error("Password is required");
                return ResponseEntity.badRequest().body(Map.of("message", "Password is required"));
            }

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
} 