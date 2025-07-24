package com.enrollment.auth.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for SecurityEventLogger to ensure compliance with security requirements.
 * Verifies implementation of:
 * - 2.4.5: Log all input validation failures
 * - 2.4.6: Log all authentication attempts
 * - 2.4.7: Log all access control failures
 */
class SecurityEventLoggerTest {

    private SecurityEventLogger securityLogger;
    private ListAppender<ILoggingEvent> listAppender;
    private Logger logger;

    @BeforeEach
    void setUp() {
        securityLogger = new SecurityEventLogger();
        
        // Setup log capture
        logger = (Logger) LoggerFactory.getLogger(SecurityEventLogger.class);
        listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
    }

    @Test
    @DisplayName("Should log successful authentication with correct format")
    void shouldLogAuthenticationSuccess() {
        String email = "test@example.com";
        String ipAddress = "192.168.1.1";
        
        securityLogger.logAuthenticationSuccess(email, ipAddress);
        
        List<ILoggingEvent> logsList = listAppender.list;
        assertEquals(1, logsList.size());
        
        ILoggingEvent logEvent = logsList.get(0);
        assertEquals(Level.INFO, logEvent.getLevel());
        assertTrue(logEvent.getFormattedMessage().contains("SECURITY_EVENT"));
        assertTrue(logEvent.getFormattedMessage().contains("type=AUTH_SUCCESS"));
        assertTrue(logEvent.getFormattedMessage().contains("email=" + email));
        assertTrue(logEvent.getFormattedMessage().contains("ip=" + ipAddress));
        assertTrue(logEvent.getFormattedMessage().contains("status=SUCCESS"));
        assertTrue(logEvent.getFormattedMessage().contains("timestamp="));
    }

    @Test
    @DisplayName("Should log failed authentication with reason")
    void shouldLogAuthenticationFailure() {
        String email = "test@example.com";
        String ipAddress = "192.168.1.1";
        String reason = "Invalid password";
        
        securityLogger.logAuthenticationFailure(email, ipAddress, reason);
        
        List<ILoggingEvent> logsList = listAppender.list;
        assertEquals(1, logsList.size());
        
        ILoggingEvent logEvent = logsList.get(0);
        assertEquals(Level.WARN, logEvent.getLevel());
        assertTrue(logEvent.getFormattedMessage().contains("SECURITY_EVENT"));
        assertTrue(logEvent.getFormattedMessage().contains("type=AUTH_FAILURE"));
        assertTrue(logEvent.getFormattedMessage().contains("email=" + email));
        assertTrue(logEvent.getFormattedMessage().contains("ip=" + ipAddress));
        assertTrue(logEvent.getFormattedMessage().contains("status=FAILED"));
        assertTrue(logEvent.getFormattedMessage().contains("reason=" + reason));
    }

    @Test
    @DisplayName("Should log account lockout events")
    void shouldLogAccountLockout() {
        String email = "test@example.com";
        int attempts = 5;
        
        securityLogger.logAccountLockout(email, attempts);
        
        List<ILoggingEvent> logsList = listAppender.list;
        assertEquals(1, logsList.size());
        
        ILoggingEvent logEvent = logsList.get(0);
        assertEquals(Level.WARN, logEvent.getLevel());
        assertTrue(logEvent.getFormattedMessage().contains("SECURITY_EVENT"));
        assertTrue(logEvent.getFormattedMessage().contains("type=ACCOUNT_LOCKOUT"));
        assertTrue(logEvent.getFormattedMessage().contains("email=" + email));
        assertTrue(logEvent.getFormattedMessage().contains("attempts=" + attempts));
        assertTrue(logEvent.getFormattedMessage().contains("status=LOCKED"));
    }

    @Test
    @DisplayName("Should log validation failures")
    void shouldLogValidationFailure() {
        String endpoint = "/api/auth/register";
        String field = "password";
        String error = "Password must be at least 8 characters";
        
        securityLogger.logValidationFailure(endpoint, field, error);
        
        List<ILoggingEvent> logsList = listAppender.list;
        assertEquals(1, logsList.size());
        
        ILoggingEvent logEvent = logsList.get(0);
        assertEquals(Level.ERROR, logEvent.getLevel());
        assertTrue(logEvent.getFormattedMessage().contains("SECURITY_EVENT"));
        assertTrue(logEvent.getFormattedMessage().contains("type=VALIDATION_FAILURE"));
        assertTrue(logEvent.getFormattedMessage().contains("endpoint=" + endpoint));
        assertTrue(logEvent.getFormattedMessage().contains("field=" + field));
        assertTrue(logEvent.getFormattedMessage().contains("error=" + error));
    }

    @Test
    @DisplayName("Should log access control failures")
    void shouldLogAccessControlFailure() {
        String email = "student@example.com";
        String resource = "/api/admin/users";
        String action = "GET";
        
        securityLogger.logAccessControlFailure(email, resource, action);
        
        List<ILoggingEvent> logsList = listAppender.list;
        assertEquals(1, logsList.size());
        
        ILoggingEvent logEvent = logsList.get(0);
        assertEquals(Level.ERROR, logEvent.getLevel());
        assertTrue(logEvent.getFormattedMessage().contains("SECURITY_EVENT"));
        assertTrue(logEvent.getFormattedMessage().contains("type=ACCESS_DENIED"));
        assertTrue(logEvent.getFormattedMessage().contains("email=" + email));
        assertTrue(logEvent.getFormattedMessage().contains("resource=" + resource));
        assertTrue(logEvent.getFormattedMessage().contains("action=" + action));
    }

    @Test
    @DisplayName("Should log password change events")
    void shouldLogPasswordChange() {
        String email = "test@example.com";
        
        securityLogger.logPasswordChange(email);
        
        List<ILoggingEvent> logsList = listAppender.list;
        assertEquals(1, logsList.size());
        
        ILoggingEvent logEvent = logsList.get(0);
        assertEquals(Level.INFO, logEvent.getLevel());
        assertTrue(logEvent.getFormattedMessage().contains("SECURITY_EVENT"));
        assertTrue(logEvent.getFormattedMessage().contains("type=PASSWORD_CHANGE"));
        assertTrue(logEvent.getFormattedMessage().contains("email=" + email));
        assertTrue(logEvent.getFormattedMessage().contains("status=SUCCESS"));
    }

    @Test
    @DisplayName("Should log password change failures")
    void shouldLogPasswordChangeFailure() {
        String email = "test@example.com";
        String reason = "Password too new";
        
        securityLogger.logPasswordChangeFailure(email, reason);
        
        List<ILoggingEvent> logsList = listAppender.list;
        assertEquals(1, logsList.size());
        
        ILoggingEvent logEvent = logsList.get(0);
        assertEquals(Level.WARN, logEvent.getLevel());
        assertTrue(logEvent.getFormattedMessage().contains("SECURITY_EVENT"));
        assertTrue(logEvent.getFormattedMessage().contains("type=PASSWORD_CHANGE_FAILURE"));
        assertTrue(logEvent.getFormattedMessage().contains("email=" + email));
        assertTrue(logEvent.getFormattedMessage().contains("reason=" + reason));
    }

    @Test
    @DisplayName("Should log security question updates")
    void shouldLogSecurityQuestionUpdate() {
        String email = "test@example.com";
        
        securityLogger.logSecurityQuestionUpdate(email);
        
        List<ILoggingEvent> logsList = listAppender.list;
        assertEquals(1, logsList.size());
        
        ILoggingEvent logEvent = logsList.get(0);
        assertEquals(Level.INFO, logEvent.getLevel());
        assertTrue(logEvent.getFormattedMessage().contains("SECURITY_EVENT"));
        assertTrue(logEvent.getFormattedMessage().contains("type=SECURITY_QUESTION_UPDATE"));
        assertTrue(logEvent.getFormattedMessage().contains("email=" + email));
    }

    @Test
    @DisplayName("Should use consistent timestamp format")
    void shouldUseConsistentTimestampFormat() {
        securityLogger.logAuthenticationSuccess("test@example.com", "127.0.0.1");
        securityLogger.logPasswordChange("test@example.com");
        
        List<ILoggingEvent> logsList = listAppender.list;
        assertEquals(2, logsList.size());
        
        // Extract timestamps and verify format (ISO format: YYYY-MM-DDTHH:MM:SS)
        for (ILoggingEvent event : logsList) {
            String message = event.getFormattedMessage();
            assertTrue(message.matches(".*timestamp=\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.*"),
                    "Timestamp should be in ISO format");
        }
    }

    @Test
    @DisplayName("Should log multiple events independently")
    void shouldLogMultipleEventsIndependently() {
        // Log various security events
        securityLogger.logAuthenticationSuccess("user1@example.com", "192.168.1.1");
        securityLogger.logAuthenticationFailure("user2@example.com", "192.168.1.2", "Invalid password");
        securityLogger.logAccountLockout("user3@example.com", 5);
        securityLogger.logValidationFailure("/api/register", "email", "Invalid format");
        
        List<ILoggingEvent> logsList = listAppender.list;
        assertEquals(4, logsList.size());
        
        // Verify each event type is logged correctly
        assertTrue(logsList.stream().anyMatch(e -> e.getFormattedMessage().contains("type=AUTH_SUCCESS")));
        assertTrue(logsList.stream().anyMatch(e -> e.getFormattedMessage().contains("type=AUTH_FAILURE")));
        assertTrue(logsList.stream().anyMatch(e -> e.getFormattedMessage().contains("type=ACCOUNT_LOCKOUT")));
        assertTrue(logsList.stream().anyMatch(e -> e.getFormattedMessage().contains("type=VALIDATION_FAILURE")));
    }
}