package com.enrollment.auth.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import com.enrollment.auth.validation.ValidPassword;

@Data
public class PasswordChangeRequest {
    @NotBlank(message = "Current password is required")
    private String currentPassword;
    
    @NotBlank(message = "New password is required")
    @ValidPassword
    private String newPassword;
}