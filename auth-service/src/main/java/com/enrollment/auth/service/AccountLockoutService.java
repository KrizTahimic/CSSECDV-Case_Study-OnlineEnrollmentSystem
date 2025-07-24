package com.enrollment.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Service for managing account lockout mechanism.
 * Implements requirement 2.1.8 - Account disabling after failed login attempts.
 * 
 * Uses Redis for fast, distributed tracking of failed attempts.
 * Accounts are locked for 15 minutes after 5 failed attempts.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AccountLockoutService {
    
    private final RedisTemplate<String, String> redisTemplate;
    
    // Maximum failed attempts before lockout
    private static final int MAX_ATTEMPTS = 5;
    // Lockout duration to discourage brute force attacks
    private static final long LOCKOUT_DURATION_MINUTES = 15;
    // Redis key prefixes for organization
    private static final String ATTEMPT_KEY_PREFIX = "login_attempt:";
    private static final String LOCKOUT_KEY_PREFIX = "account_locked:";
    
    /**
     * Records a failed login attempt for the given email.
     * Increments the attempt counter and locks the account if max attempts reached.
     * 
     * @param email The email of the account that failed to login
     */
    public void recordFailedAttempt(String email) {
        String attemptKey = ATTEMPT_KEY_PREFIX + email;
        String lockoutKey = LOCKOUT_KEY_PREFIX + email;
        
        // Increment failed attempts counter in Redis
        Long attempts = redisTemplate.opsForValue().increment(attemptKey);
        
        if (attempts == null) {
            attempts = 1L;
        }
        
        log.info("Failed login attempt {} for email: {}", attempts, email);
        
        // Set expiration for attempt counter (reset after 15 minutes if not locked)
        redisTemplate.expire(attemptKey, Duration.ofMinutes(LOCKOUT_DURATION_MINUTES));
        
        // Lock account if max attempts reached
        if (attempts >= MAX_ATTEMPTS) {
            log.warn("Account locked due to {} failed attempts for email: {}", attempts, email);
            redisTemplate.opsForValue().set(lockoutKey, LocalDateTime.now().toString(), 
                Duration.ofMinutes(LOCKOUT_DURATION_MINUTES));
        }
    }
    
    /**
     * Resets failed login attempts for a user after successful login.
     * 
     * @param email The email of the account to reset
     */
    public void resetFailedAttempts(String email) {
        String attemptKey = ATTEMPT_KEY_PREFIX + email;
        redisTemplate.delete(attemptKey);
        log.info("Reset failed login attempts for email: {}", email);
    }
    
    /**
     * Checks if an account is currently locked due to failed attempts.
     * 
     * @param email The email to check
     * @return true if account is locked, false otherwise
     */
    public boolean isAccountLocked(String email) {
        String lockoutKey = LOCKOUT_KEY_PREFIX + email;
        Boolean hasKey = redisTemplate.hasKey(lockoutKey);
        return hasKey != null && hasKey;
    }
    
    /**
     * Gets the current number of failed attempts for an account.
     * 
     * @param email The email to check
     * @return Number of failed attempts, 0 if none
     */
    public int getFailedAttempts(String email) {
        String attemptKey = ATTEMPT_KEY_PREFIX + email;
        String attempts = redisTemplate.opsForValue().get(attemptKey);
        return attempts != null ? Integer.parseInt(attempts) : 0;
    }
}