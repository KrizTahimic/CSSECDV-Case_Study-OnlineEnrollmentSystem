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
        try {
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
        } catch (Exception e) {
            // Fail closed - maintain security even if Redis is unavailable
            log.error("Critical: Unable to record failed login attempt for {}. Redis unavailable.", email, e);
            throw new RuntimeException("Service temporarily unavailable");
        }
    }
    
    /**
     * Resets failed login attempts for a user after successful login.
     * Clears both the attempt counter and any lockout status.
     * 
     * @param email The email of the account to reset
     */
    public void resetFailedAttempts(String email) {
        try {
            String attemptKey = ATTEMPT_KEY_PREFIX + email;
            String lockoutKey = LOCKOUT_KEY_PREFIX + email;
            
            // Clear both attempts and lockout status
            redisTemplate.delete(attemptKey);
            redisTemplate.delete(lockoutKey);
            
            log.info("Reset failed login attempts for email: {}", email);
        } catch (Exception e) {
            // Log error but don't fail - successful login should proceed
            log.error("Warning: Unable to reset failed attempts for {}. Redis may be unavailable.", email, e);
            // Don't throw - this is called after successful authentication
        }
    }
    
    /**
     * Checks if an account is currently locked due to failed attempts.
     * Fails closed - if Redis is unavailable, assumes account is locked for security.
     * 
     * @param email The email to check
     * @return true if account is locked, false otherwise
     * @throws RuntimeException if Redis is unavailable
     */
    public boolean isAccountLocked(String email) {
        try {
            String lockoutKey = LOCKOUT_KEY_PREFIX + email;
            String lockTime = redisTemplate.opsForValue().get(lockoutKey);
            
            if (lockTime != null) {
                // Verify the lock is still valid
                try {
                    LocalDateTime lockDateTime = LocalDateTime.parse(lockTime);
                    LocalDateTime unlockTime = lockDateTime.plusMinutes(LOCKOUT_DURATION_MINUTES);
                    
                    if (LocalDateTime.now().isBefore(unlockTime)) {
                        return true;
                    } else {
                        // Lock expired, clean it up
                        redisTemplate.delete(lockoutKey);
                        return false;
                    }
                } catch (Exception e) {
                    // If we can't parse the lock time, assume locked
                    log.error("Invalid lock time format for {}: {}", email, lockTime);
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            // Fail closed - assume locked if Redis is unavailable
            log.error("Critical: Unable to check account lock status for {}. Redis unavailable.", email, e);
            throw new RuntimeException("Service temporarily unavailable");
        }
    }
    
    /**
     * Gets the current number of failed attempts for an account.
     * 
     * @param email The email to check
     * @return Number of failed attempts, 0 if none or on error
     */
    public int getFailedAttempts(String email) {
        String attemptKey = ATTEMPT_KEY_PREFIX + email;
        try {
            String attempts = redisTemplate.opsForValue().get(attemptKey);
            return attempts != null ? Integer.parseInt(attempts) : 0;
        } catch (NumberFormatException e) {
            log.error("Invalid attempt count in Redis for email: {}", email, e);
            return 0;
        } catch (Exception e) {
            log.error("Error retrieving failed attempts for email: {}", email, e);
            return 0;
        }
    }
}