package com.enrollment.auth.dto;

import lombok.Data;
import jakarta.validation.constraints.*;
import com.enrollment.auth.validation.ValidPassword;

@Data
public class RegisterRequest {
    @NotBlank(message = "First name is required")
    private String firstName;
    
    @NotBlank(message = "Last name is required")
    private String lastName;
    
    @NotBlank(message = "Password is required")
    @ValidPassword
    private String password;
    
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;
    
    @NotBlank(message = "Role is required")
    @Pattern(regexp = "^(student|faculty|Faculty|instructor|admin)$", message = "Invalid role")
    private String role;
    
    // For security questions (requirement 2.1.9)
    private String securityQuestion;
    private String securityAnswer;
} 