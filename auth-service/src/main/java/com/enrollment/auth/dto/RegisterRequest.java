package com.enrollment.auth.dto;

import lombok.Data;

@Data
public class RegisterRequest {
    private String firstName;
    private String lastName;
    private String password;
    private String email;
    private String role;
} 