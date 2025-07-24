package com.enrollment.auth.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import lombok.extern.slf4j.Slf4j;
import com.enrollment.auth.service.SecurityEventLogger;
import lombok.RequiredArgsConstructor;
import jakarta.servlet.http.HttpServletRequest;

import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for the Auth Service.
 * Implements requirements:
 * - 2.1.4: Generic error messages (no information leakage)
 * - 2.4.1: No debugging/stack trace information exposed
 * - 2.4.2: Generic error messages and custom error pages
 * - 2.4.5: Log all input validation failures
 */
@RestControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class GlobalExceptionHandler {
    
    private final SecurityEventLogger securityLogger;
    
    /**
     * Handles validation exceptions from @Valid annotations.
     * Logs specific errors for debugging but returns generic message to client.
     * 
     * @param ex The validation exception
     * @return Generic error response
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidationExceptions(MethodArgumentNotValidException ex, 
                                                       HttpServletRequest request) {
        // Log the specific validation errors for debugging (requirement 2.4.5)
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            log.error("Validation failed for field '{}': {}", fieldName, errorMessage);
            
            // Log to security logger
            securityLogger.logValidationFailure(request.getRequestURI(), fieldName, errorMessage);
        });
        
        // Return generic error message to avoid revealing system details (requirement 2.1.4)
        Map<String, String> response = new HashMap<>();
        response.put("message", "Invalid input data provided");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
    
    /**
     * Handles all other exceptions to prevent stack trace exposure.
     * 
     * @param ex The exception
     * @return Generic error response
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGenericException(Exception ex) {
        log.error("Unexpected error occurred", ex);
        
        // Generic error message (requirement 2.4.2)
        Map<String, String> response = new HashMap<>();
        response.put("message", "An error occurred while processing your request");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}