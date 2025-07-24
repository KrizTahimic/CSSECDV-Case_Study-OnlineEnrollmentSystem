package com.enrollment.auth.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

/**
 * Custom validation annotation for password complexity requirements.
 * Ensures passwords meet security standards as per requirement 2.1.5 and 2.1.6.
 * 
 * Password must contain:
 * - Minimum 8 characters
 * - At least 1 uppercase letter
 * - At least 1 lowercase letter  
 * - At least 1 number
 * - At least 1 special character
 */
@Documented
@Constraint(validatedBy = PasswordValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidPassword {
    String message() default "Password must be at least 8 characters long and contain at least one uppercase letter, one lowercase letter, one number, and one special character";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}