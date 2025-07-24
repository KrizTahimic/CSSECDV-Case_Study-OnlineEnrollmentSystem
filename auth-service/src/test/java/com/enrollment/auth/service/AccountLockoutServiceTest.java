package com.enrollment.auth.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests for AccountLockoutService to ensure compliance with security requirements.
 * Verifies implementation of:
 * - 2.1.8: Account lockout after 5 failed attempts with 15-minute lockout period
 */
@ExtendWith(MockitoExtension.class)
class AccountLockoutServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private AccountLockoutService lockoutService;

    private static final String TEST_EMAIL = "test@example.com";
    private static final String ATTEMPT_KEY = "login_attempt:test@example.com";
    private static final String LOCKOUT_KEY = "account_locked:test@example.com";

    @BeforeEach
    void setUp() {
        lockoutService = new AccountLockoutService(redisTemplate);
    }

    @Test
    @DisplayName("Should track failed login attempts")
    void shouldTrackFailedAttempts() {
        // Setup for this test
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        
        // First attempt
        when(valueOperations.increment(ATTEMPT_KEY)).thenReturn(1L);
        
        lockoutService.recordFailedAttempt(TEST_EMAIL);
        
        verify(valueOperations).increment(ATTEMPT_KEY);
        verify(redisTemplate).expire(ATTEMPT_KEY, Duration.ofMinutes(15));
        verify(valueOperations, never()).set(anyString(), anyString(), any(Duration.class));
    }

    @Test
    @DisplayName("Should lock account after 5 failed attempts")
    void shouldLockAccountAfterFiveFailedAttempts() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        
        // Fifth attempt triggers lockout
        when(valueOperations.increment(ATTEMPT_KEY)).thenReturn(5L);
        
        lockoutService.recordFailedAttempt(TEST_EMAIL);
        
        verify(valueOperations).increment(ATTEMPT_KEY);
        verify(valueOperations).set(eq(LOCKOUT_KEY), anyString(), eq(Duration.ofMinutes(15)));
    }

    @Test
    @DisplayName("Should maintain lockout even after more than 5 attempts")
    void shouldMaintainLockoutAfterMoreAttempts() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        
        // Sixth attempt
        when(valueOperations.increment(ATTEMPT_KEY)).thenReturn(6L);
        
        lockoutService.recordFailedAttempt(TEST_EMAIL);
        
        verify(valueOperations).increment(ATTEMPT_KEY);
        verify(valueOperations).set(eq(LOCKOUT_KEY), anyString(), eq(Duration.ofMinutes(15)));
    }

    @Test
    @DisplayName("Should correctly identify locked accounts")
    void shouldIdentifyLockedAccounts() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        
        // Account is locked with valid time
        LocalDateTime lockTime = LocalDateTime.now();
        when(valueOperations.get(LOCKOUT_KEY)).thenReturn(lockTime.toString());
        
        assertTrue(lockoutService.isAccountLocked(TEST_EMAIL));
        
        // Account is locked with expired time
        LocalDateTime expiredLockTime = LocalDateTime.now().minusMinutes(20);
        when(valueOperations.get(LOCKOUT_KEY)).thenReturn(expiredLockTime.toString());
        
        assertFalse(lockoutService.isAccountLocked(TEST_EMAIL));
        verify(redisTemplate).delete(LOCKOUT_KEY); // Should clean up expired lock
        
        // Account is not locked
        when(valueOperations.get(LOCKOUT_KEY)).thenReturn(null);
        
        assertFalse(lockoutService.isAccountLocked(TEST_EMAIL));
    }

    @Test
    @DisplayName("Should reset failed attempts on successful login")
    void shouldResetFailedAttempts() {
        // No mocking needed for this test
        lockoutService.resetFailedAttempts(TEST_EMAIL);
        
        verify(redisTemplate).delete(ATTEMPT_KEY);
        verify(redisTemplate).delete(LOCKOUT_KEY);
    }

    @Test
    @DisplayName("Should return current failed attempt count")
    void shouldReturnFailedAttemptCount() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        
        // Has 3 failed attempts
        when(valueOperations.get(ATTEMPT_KEY)).thenReturn("3");
        
        assertEquals(3, lockoutService.getFailedAttempts(TEST_EMAIL));
        
        // No failed attempts
        when(valueOperations.get(ATTEMPT_KEY)).thenReturn(null);
        
        assertEquals(0, lockoutService.getFailedAttempts(TEST_EMAIL));
        
        // Invalid number format should return 0 (handled gracefully)
        when(valueOperations.get(ATTEMPT_KEY)).thenReturn("invalid");
        
        assertEquals(0, lockoutService.getFailedAttempts(TEST_EMAIL));
        
        // Redis error should return 0 (handled gracefully)
        when(valueOperations.get(ATTEMPT_KEY)).thenThrow(new RuntimeException("Redis error"));
        
        assertEquals(0, lockoutService.getFailedAttempts(TEST_EMAIL));
    }

    @Test
    @DisplayName("Should handle multiple users independently")
    void shouldHandleMultipleUsersIndependently() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        
        String user1 = "user1@example.com";
        String user2 = "user2@example.com";
        
        // User 1 has 3 attempts
        when(valueOperations.increment("login_attempt:" + user1)).thenReturn(3L);
        // User 2 has 5 attempts (should lock)
        when(valueOperations.increment("login_attempt:" + user2)).thenReturn(5L);
        
        lockoutService.recordFailedAttempt(user1);
        lockoutService.recordFailedAttempt(user2);
        
        // Verify user1 not locked
        verify(valueOperations, never()).set(eq("account_locked:" + user1), anyString(), any(Duration.class));
        
        // Verify user2 is locked
        verify(valueOperations).set(eq("account_locked:" + user2), anyString(), eq(Duration.ofMinutes(15)));
    }

    @Test
    @DisplayName("Should set correct expiration times")
    void shouldSetCorrectExpirationTimes() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        
        // Test attempt expiration
        when(valueOperations.increment(ATTEMPT_KEY)).thenReturn(1L);
        
        lockoutService.recordFailedAttempt(TEST_EMAIL);
        
        verify(redisTemplate).expire(ATTEMPT_KEY, Duration.ofMinutes(15));
        
        // Test lockout expiration
        when(valueOperations.increment(ATTEMPT_KEY)).thenReturn(5L);
        
        lockoutService.recordFailedAttempt(TEST_EMAIL);
        
        verify(valueOperations).set(eq(LOCKOUT_KEY), anyString(), eq(Duration.ofMinutes(15)));
    }

    @Test
    @DisplayName("Should handle Redis exceptions with fail-closed approach")
    void shouldHandleRedisExceptionsWithFailClosed() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        
        // Simulate Redis connection failure for recordFailedAttempt
        when(valueOperations.increment(ATTEMPT_KEY)).thenThrow(new RuntimeException("Redis connection failed"));
        
        // Should throw exception (fail closed)
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> lockoutService.recordFailedAttempt(TEST_EMAIL));
        assertEquals("Service temporarily unavailable", exception.getMessage());
        
        // For isAccountLocked, should throw exception on error (fail closed)
        when(valueOperations.get(LOCKOUT_KEY)).thenThrow(new RuntimeException("Redis connection failed"));
        
        exception = assertThrows(RuntimeException.class, 
            () -> lockoutService.isAccountLocked(TEST_EMAIL));
        assertEquals("Service temporarily unavailable", exception.getMessage());
    }

    @Test
    @DisplayName("Should track attempts from 1 to 5 correctly")
    void shouldTrackAttemptsProgressively() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        
        // Simulate progressive failed attempts
        for (int i = 1; i <= 5; i++) {
            when(valueOperations.increment(ATTEMPT_KEY)).thenReturn((long) i);
            
            lockoutService.recordFailedAttempt(TEST_EMAIL);
            
            if (i < 5) {
                // Before 5 attempts, should not lock
                verify(valueOperations, never()).set(eq(LOCKOUT_KEY), anyString(), any(Duration.class));
            } else {
                // At 5 attempts, should lock
                verify(valueOperations).set(eq(LOCKOUT_KEY), anyString(), eq(Duration.ofMinutes(15)));
            }
        }
    }
}