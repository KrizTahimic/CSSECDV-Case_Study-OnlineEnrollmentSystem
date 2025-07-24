# Security Implementation Guide

This document explains the security controls implemented in the Online Enrollment System to meet CSSECDV requirements.

## Overview

The security implementation focuses on authentication hardening, input validation, account protection, and proper error handling across the microservices architecture.

## Implemented Security Controls

### 1. Authentication (Section 2.1)

#### 1.1 Generic Error Messages (2.1.4)
- **Location**: `AuthService.java`, `AuthController.java`
- **Implementation**: All authentication failures return "Invalid username and/or password"
- **Purpose**: Prevents attackers from determining if username exists

#### 1.2 Password Complexity (2.1.5, 2.1.6)
- **Location**: `ValidPassword.java`, `PasswordValidator.java`
- **Requirements**:
  - Minimum 8 characters
  - At least 1 uppercase letter
  - At least 1 lowercase letter
  - At least 1 number
  - At least 1 special character
- **Applied**: Registration and password change endpoints

#### 1.3 Password Display (2.1.7)
- **Implementation**: Frontend responsibility (use input type="password")
- **Backend**: Passwords never returned in responses

#### 1.4 Account Lockout (2.1.8)
- **Location**: `AccountLockoutService.java`
- **Configuration**:
  - 5 failed attempts trigger lockout
  - 15-minute lockout duration
  - Uses Redis for distributed tracking
- **Purpose**: Prevent brute force attacks

#### 1.5 Security Questions (2.1.9)
- **Location**: `SecurityQuestions.java`, `User.java`
- **Implementation**: 
  - 10 predefined questions with good entropy
  - Answers hashed like passwords
  - Required during registration

#### 1.6 Password History (2.1.10)
- **Location**: `User.java` (passwordHistory field)
- **Implementation**: Stores last 5 password hashes
- **Check**: Password change endpoint validates against history

#### 1.7 Password Age (2.1.11)
- **Location**: `User.java` (passwordChangedAt field)
- **Implementation**: 24-hour minimum before password change allowed

#### 1.8 Last Login Tracking (2.1.12)
- **Location**: `AuthService.login()`, `AuthResponse.java`
- **Features**:
  - Tracks last login time and IP
  - Returns previous login info on successful login
  - Stores both current and previous login details

#### 1.9 Re-authentication (2.1.13)
- **Endpoint**: `/api/auth/reauthenticate`
- **Used for**: Password changes, sensitive operations
- **Implementation**: Verifies current password before allowing action

### 2. Authorization/Access Control (Section 2.2)

#### 2.1 Centralized Component (2.2.1)
- **Note**: Basic implementation - relies on JWT roles
- **TODO**: Implement Spring Security method-level authorization

#### 2.2 Fail Securely (2.2.2)
- **Implementation**: Default deny - endpoints require authentication
- **TODO**: Add @PreAuthorize annotations to controllers

### 3. Data Validation (Section 2.3)

#### 3.1 Input Validation (2.3.1)
- **Location**: DTOs with Jakarta validation annotations
- **Implementation**: 
  - `@NotBlank`, `@Email`, `@Pattern` annotations
  - Custom `@ValidPassword` annotation
  - Validation failures reject input (no sanitization)

#### 3.2 Range & Length Validation (2.3.2, 2.3.3)
- **Implementation**: Through validation annotations
- **Examples**:
  - Password: 8+ characters
  - Email: Valid format
  - Role: Specific allowed values

### 4. Error Handling & Logging (Section 2.4)

#### 4.1 No Stack Traces (2.4.1)
- **Location**: `GlobalExceptionHandler.java`
- **Implementation**: Catches all exceptions, returns generic messages

#### 4.2 Generic Error Messages (2.4.2)
- **Implementation**: 
  - Validation errors: "Invalid input data provided"
  - Server errors: "An error occurred while processing your request"

#### 4.3 Security Event Logging (2.4.5, 2.4.6, 2.4.7)
- **Current**: Basic logging with SLF4J
- **Logs**:
  - All validation failures
  - All authentication attempts
  - Account lockout events
- **TODO**: Implement structured security event logger

#### 4.4 Log Access (2.4.4)
- **TODO**: Create admin-only endpoint for log viewing

## Configuration Required

### Redis Setup
```yaml
spring:
  redis:
    host: localhost
    port: 6379
```

### MongoDB Indexes
The User model requires unique index on email field (already configured).

## Testing the Implementation

### 1. Test Generic Error Messages
```bash
# Invalid username
curl -X POST http://localhost:3001/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"nonexistent@test.com","password":"wrong"}'
# Should return: "Invalid username and/or password"

# Invalid password  
curl -X POST http://localhost:3001/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"valid@test.com","password":"wrong"}'
# Should return: "Invalid username and/or password"
```

### 2. Test Password Validation
```bash
# Weak password
curl -X POST http://localhost:3001/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"test@test.com","password":"weak","firstName":"Test","lastName":"User","role":"student"}'
# Should return: "Invalid input data provided"
```

### 3. Test Account Lockout
```bash
# Make 5 failed login attempts
# 6th attempt should fail even with correct password
# Wait 15 minutes for unlock
```

### 4. Test Last Login Display
```bash
# Login twice and check response includes lastLoginTime and lastLoginIP
```

## Security Considerations

1. **Redis Security**: Ensure Redis is not exposed publicly
2. **JWT Secret**: Use strong, unique secret per environment
3. **HTTPS**: Always use HTTPS in production
4. **CORS**: Configure appropriately for your frontend domain
5. **Rate Limiting**: Consider adding general rate limiting beyond lockout

## Remaining TODOs

1. Add method-level authorization (@PreAuthorize)
2. Implement password change endpoint with all checks
3. Create admin log viewing endpoint
4. Add structured security event logging
5. Implement password reset with security questions
6. Add CSRF protection if using session cookies

## Compliance Summary

- ✅ Authentication controls (2.1.1-2.1.13): Mostly complete
- ⚠️ Authorization controls (2.2.1-2.2.3): Basic implementation
- ✅ Data validation (2.3.1-2.3.3): Complete
- ⚠️ Error handling & logging (2.4.1-2.4.7): Partial implementation

This implementation provides a solid security foundation while keeping complexity minimal for academic requirements.