package com.enrollment.auth.validation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import jakarta.validation.ConstraintValidatorContext;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * Tests for PasswordValidator to ensure compliance with security requirements.
 * Verifies implementation of:
 * - 2.1.5: Password complexity requirements
 * - 2.1.6: Password length requirements (minimum 8 characters)
 */
class PasswordValidatorTest {

    private PasswordValidator validator;
    private ConstraintValidatorContext context;

    @BeforeEach
    void setUp() {
        validator = new PasswordValidator();
        context = mock(ConstraintValidatorContext.class);
    }

    @Test
    @DisplayName("Should accept valid password with all requirements")
    void shouldAcceptValidPassword() {
        // Password with uppercase, lowercase, number, special char, and 8+ length
        String validPassword = "Test@123";
        assertTrue(validator.isValid(validPassword, context),
                "Password meeting all requirements should be valid");
    }

    @ParameterizedTest
    @DisplayName("Should accept various valid passwords")
    @ValueSource(strings = {
            "Password@1",      // Basic valid password
            "Complex#Pass99",  // Longer password
            "Ab1!efgh",       // Minimum length with all requirements
            "Test$2024Pass",  // Mixed case with special in middle
            "P@ssw0rd123"     // Common pattern but valid
    })
    void shouldAcceptVariousValidPasswords(String password) {
        assertTrue(validator.isValid(password, context),
                "Password '" + password + "' should be valid");
    }

    @Test
    @DisplayName("Should reject null password")
    void shouldRejectNullPassword() {
        assertFalse(validator.isValid(null, context),
                "Null password should be invalid");
    }

    @Test
    @DisplayName("Should reject empty password")
    void shouldRejectEmptyPassword() {
        assertFalse(validator.isValid("", context),
                "Empty password should be invalid");
    }

    @ParameterizedTest
    @DisplayName("Should reject passwords shorter than 8 characters")
    @ValueSource(strings = {
            "Test@1",    // 6 chars
            "Pass#1",    // 6 chars
            "Ab1!efg",   // 7 chars
            "T@st1"      // 5 chars
    })
    void shouldRejectShortPasswords(String password) {
        assertFalse(validator.isValid(password, context),
                "Password shorter than 8 characters should be invalid");
    }

    @Test
    @DisplayName("Should reject password without uppercase letter")
    void shouldRejectPasswordWithoutUppercase() {
        String password = "test@123"; // No uppercase
        assertFalse(validator.isValid(password, context),
                "Password without uppercase letter should be invalid");
    }

    @Test
    @DisplayName("Should reject password without lowercase letter")
    void shouldRejectPasswordWithoutLowercase() {
        String password = "TEST@123"; // No lowercase
        assertFalse(validator.isValid(password, context),
                "Password without lowercase letter should be invalid");
    }

    @Test
    @DisplayName("Should reject password without number")
    void shouldRejectPasswordWithoutNumber() {
        String password = "Test@Pass"; // No number
        assertFalse(validator.isValid(password, context),
                "Password without number should be invalid");
    }

    @Test
    @DisplayName("Should reject password without special character")
    void shouldRejectPasswordWithoutSpecialCharacter() {
        String password = "TestPass123"; // No special character
        assertFalse(validator.isValid(password, context),
                "Password without special character should be invalid");
    }

    @ParameterizedTest
    @DisplayName("Should reject passwords with only whitespace special characters")
    @ValueSource(strings = {
            "Test 123A",      // Space is whitespace, not special
            "Test\t123A",     // Tab is whitespace, not special
            "Test\n123A"      // Newline is whitespace, not special
    })
    void shouldRejectPasswordsWithOnlyWhitespace(String password) {
        assertFalse(validator.isValid(password, context),
                "Password with only whitespace should be invalid");
    }

    @Test
    @DisplayName("Should accept various special characters")
    void shouldAcceptVariousSpecialCharacters() {
        String[] specialChars = {"!", "@", "#", "$", "%", "^", "&", "*", "(", ")", 
                                "-", "_", "=", "+", "[", "]", "{", "}", ";", ":", 
                                "'", "\"", ",", ".", "<", ">", "?", "/", "|", "\\"};
        
        for (String special : specialChars) {
            String password = "Test123" + special;
            assertTrue(validator.isValid(password, context),
                    "Password with special character '" + special + "' should be valid");
        }
    }

    @Test
    @DisplayName("Should validate edge case of exactly 8 characters")
    void shouldValidateExactlyEightCharacters() {
        String validEightChar = "Test@12A"; // Exactly 8 chars with all requirements
        assertTrue(validator.isValid(validEightChar, context),
                "8-character password meeting all requirements should be valid");
        
        String invalidSevenChar = "Test@1A"; // 7 chars
        assertFalse(validator.isValid(invalidSevenChar, context),
                "7-character password should be invalid");
    }
}