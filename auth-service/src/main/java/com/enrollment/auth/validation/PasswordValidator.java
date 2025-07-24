package com.enrollment.auth.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validator implementation for password complexity requirements.
 * Implements CSSECDV security requirements 2.1.5 and 2.1.6.
 */
public class PasswordValidator implements ConstraintValidator<ValidPassword, String> {
    
    @Override
    public void initialize(ValidPassword constraintAnnotation) {
        // No initialization needed
    }
    
    /**
     * Validates password against complexity requirements.
     * 
     * @param password The password to validate
     * @param context The validation context
     * @return true if password meets all requirements, false otherwise
     */
    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (password == null) {
            return false;
        }
        
        // Check minimum length (requirement 2.1.6)
        if (password.length() < 8) {
            return false;
        }
        
        // Check for at least one uppercase letter
        boolean hasUppercase = password.chars().anyMatch(Character::isUpperCase);
        
        // Check for at least one lowercase letter
        boolean hasLowercase = password.chars().anyMatch(Character::isLowerCase);
        
        // Check for at least one digit
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        
        // Check for at least one special character
        // Special characters are any non-alphanumeric, non-whitespace characters
        boolean hasSpecial = password.chars().anyMatch(ch -> 
            !Character.isLetterOrDigit(ch) && !Character.isWhitespace(ch));
        
        // All conditions must be met for valid password
        return hasUppercase && hasLowercase && hasDigit && hasSpecial;
    }
}