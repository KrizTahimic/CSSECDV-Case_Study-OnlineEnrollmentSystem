package com.enrollment.auth.service;

import com.enrollment.auth.dto.AuthRequest;
import com.enrollment.auth.dto.AuthResponse;
import com.enrollment.auth.dto.RegisterRequest;
import com.enrollment.auth.model.User;
import com.enrollment.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.io.Decoders;
import org.springframework.beans.factory.annotation.Value;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Arrays;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    
    @Value("${jwt.secret}")
    private String secret;
    
    @Value("${jwt.expiration}")
    private long expiration;

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

            // Create new user
            log.info("Creating new user with email: {}", request.getEmail());
            User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .role(role)
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

    public AuthResponse login(AuthRequest request) {
        log.info("Starting login process for email: {}", request.getEmail());
        
        try {
            // Find user
            User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    log.error("User not found with email: {}", request.getEmail());
                    return new RuntimeException("Invalid credentials");
                });

            // Verify password
            if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                log.error("Invalid password for user: {}", request.getEmail());
                throw new RuntimeException("Invalid credentials");
            }

            // Generate token
            String token = generateToken(user);
            log.info("Login successful for user: {}", request.getEmail());

            return AuthResponse.builder()
                .token(token)
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole())
                .build();
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
            claims.put("roles", List.of(user.getRole()));
            
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
} 