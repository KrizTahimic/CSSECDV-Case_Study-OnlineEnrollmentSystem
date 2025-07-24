package com.enrollment.auth.model;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "users")
public class User {
    @Id
    private String id;

    @Indexed(unique = true)
    private String email;

    private String firstName;
    private String lastName;
    private String password;
    private String role;
    
    // Security question for password reset (requirement 2.1.9)
    private String securityQuestion;
    private String securityAnswer; // This will be hashed like password
    
    // Account lockout fields (requirement 2.1.8)
    @Builder.Default
    private int failedLoginAttempts = 0;
    private LocalDateTime accountLockedUntil;
    
    // Password history (requirement 2.1.10)
    @Builder.Default
    private List<String> passwordHistory = new ArrayList<>();
    
    // Password age tracking (requirement 2.1.11)
    private LocalDateTime passwordChangedAt;
    
    // Last login tracking (requirement 2.1.12)
    private LocalDateTime lastLoginTime;
    private String lastLoginIP;
    private LocalDateTime previousLoginTime;
    private String previousLoginIP;
} 