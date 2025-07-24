package com.enrollment.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Service for logging security events.
 * Implements requirements:
 * - 2.4.5: Log all input validation failures
 * - 2.4.6: Log all authentication attempts
 * - 2.4.7: Log all access control failures
 * 
 * Uses structured logging format for easy parsing and monitoring.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SecurityEventLogger {
    
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    
    /**
     * Logs successful authentication attempts.
     */
    public void logAuthenticationSuccess(String email, String ipAddress) {
        log.info("SECURITY_EVENT type=AUTH_SUCCESS email={} ip={} timestamp={} status=SUCCESS", 
            email, ipAddress, LocalDateTime.now().format(TIMESTAMP_FORMAT));
    }
    
    /**
     * Logs failed authentication attempts.
     */
    public void logAuthenticationFailure(String email, String ipAddress, String reason) {
        log.warn("SECURITY_EVENT type=AUTH_FAILURE email={} ip={} timestamp={} status=FAILED reason={}", 
            email, ipAddress, LocalDateTime.now().format(TIMESTAMP_FORMAT), reason);
    }
    
    /**
     * Logs account lockout events.
     */
    public void logAccountLockout(String email, int attempts) {
        log.warn("SECURITY_EVENT type=ACCOUNT_LOCKOUT email={} attempts={} timestamp={} status=LOCKED", 
            email, attempts, LocalDateTime.now().format(TIMESTAMP_FORMAT));
    }
    
    /**
     * Logs validation failures.
     */
    public void logValidationFailure(String endpoint, String field, String error) {
        log.error("SECURITY_EVENT type=VALIDATION_FAILURE endpoint={} field={} error={} timestamp={}", 
            endpoint, field, error, LocalDateTime.now().format(TIMESTAMP_FORMAT));
    }
    
    /**
     * Logs access control failures.
     */
    public void logAccessControlFailure(String email, String resource, String action) {
        log.error("SECURITY_EVENT type=ACCESS_DENIED email={} resource={} action={} timestamp={}", 
            email, resource, action, LocalDateTime.now().format(TIMESTAMP_FORMAT));
    }
    
    /**
     * Logs password change events.
     */
    public void logPasswordChange(String email) {
        log.info("SECURITY_EVENT type=PASSWORD_CHANGE email={} timestamp={} status=SUCCESS", 
            email, LocalDateTime.now().format(TIMESTAMP_FORMAT));
    }
    
    /**
     * Logs password change failures.
     */
    public void logPasswordChangeFailure(String email, String reason) {
        log.warn("SECURITY_EVENT type=PASSWORD_CHANGE_FAILURE email={} reason={} timestamp={}", 
            email, reason, LocalDateTime.now().format(TIMESTAMP_FORMAT));
    }
    
    /**
     * Logs security question updates.
     */
    public void logSecurityQuestionUpdate(String email) {
        log.info("SECURITY_EVENT type=SECURITY_QUESTION_UPDATE email={} timestamp={}", 
            email, LocalDateTime.now().format(TIMESTAMP_FORMAT));
    }
}