# Manual Testing Guide - Security Features Verification

This guide provides manual testing procedures to verify the security features implemented in the Online Enrollment System according to the project checklist.

## 🚀 **PRIORITY TESTS** - Check These First for Demo

These are the core features most likely to be tested during project demonstrations:

### **HIGH PRIORITY** ⭐
- [Test 2.1.1: Authentication Required](#211-authentication-required)
- [Test 2.1.3: Password Storage Security](#213-password-storage-security)
- [Test 2.1.5/2.1.6: Password Complexity](#2152.16-password-complexity-and-length)
- [Test 2.1.8: Account Lockout](#218-account-lockout-mechanism)
- [Test 2.2.1/2.2.2: Access Control](#2212.22-access-control)

### **MEDIUM PRIORITY** ⚠️
- [Test 2.1.4: Generic Error Messages](#214-generic-error-messages)
- [Test 2.1.12: Last Login Information](#2112-last-login-reporting)
- [Test 2.3: Data Validation](#23-data-validation)

---

## Pre-Demo Requirements (Section 1)

### 1.1 Test Accounts Setup
**Spec Reference:** Checklist items 1.1.1 - 1.1.3

Create at least one account for each user type:

#### 1.1.1 Website Administrator
- **Test:** Register an admin account
- **URL:** http://localhost:3000/register
- **Steps:**
  1. Fill form with role "admin"
  2. Use valid email format
  3. Create strong password (8+ chars, uppercase, lowercase, number, special char)
  4. Submit registration
- **Expected:** Account created successfully

#### 1.1.2 Product Manager (Faculty/Instructor)
- **Test:** Register a faculty account
- **URL:** http://localhost:3000/register
- **Steps:**
  1. Fill form with role "faculty" or "instructor"
  2. Complete registration process
- **Expected:** Faculty account created

#### 1.1.3 Customer (Student)
- **Test:** Register a student account
- **URL:** http://localhost:3000/register
- **Steps:**
  1. Fill form with role "student"
  2. Complete registration process
- **Expected:** Student account created

---

## Authentication Testing (Section 2.1)

### 2.1.1 Authentication Required ⭐
**Spec Reference:** Checklist item 2.1.1

- **Test:** Access protected resources without authentication
- **Steps:**
  1. Open browser in incognito mode
  2. Try to access: http://localhost:3000/dashboard
  3. Try to access: http://localhost:3000/courses
  4. Try to access: http://localhost:3000/grades
- **Expected:** Redirected to login page
- **File Reference:** frontend/src/components/ProtectedRoute.jsx
- PASS

### 2.1.2 Secure Authentication Failure
**Spec Reference:** Checklist item 2.1.2

- **Test:** Authentication controls fail securely
- **Steps:**
  1. Attempt login with invalid credentials
  2. Check browser network tab for response
  3. Verify no sensitive information leaked
- **Expected:** Generic error message, no stack traces or debugging info

### 2.1.3 Password Storage Security ⭐
**Spec Reference:** Checklist item 2.1.3

- **Test:** Verify passwords are hashed and salted
- **Database Check:**
`mongosh auth_service --eval "db.users.find().pretty()" --quiet`
  1. Access MongoDB directly: 
  2. Query users collection: 
  3. Examine password field
- **Expected:** Password field contains BCrypt hash (starts with `$2a$` or `$2b$`)
- **File Reference:** auth-service/src/main/java/com/enrollment/auth/service/AuthService.java:102
- PASS

### 2.1.4 Generic Error Messages ⚠️
**Spec Reference:** Checklist item 2.1.4

- **Test:** Verify authentication errors are generic
- **Steps:**
  1. Login with non-existent email
  2. Login with valid email but wrong password
  3. Check error messages in both cases
- **Expected:** Both show "Invalid username and/or password"
- **File Reference:** auth-service/src/main/java/com/enrollment/auth/controller/AuthController.java:68
- PASS

### 2.1.5/2.1.6 Password Complexity and Length ⭐
**Spec Reference:** Checklist items 2.1.5, 2.1.6

- **Test:** Password validation rules
- **URL:** http://localhost:3000/register
- **Test Cases:**
  1. Password < 8 characters: "Test123"
  2. No uppercase: "test123!"
  3. No lowercase: "TEST123!"
  4. No numbers: "TestPass!"
  5. No special characters: "TestPass123"
  6. Valid password: "TestPass123!"
- **Expected:** First 5 should fail validation, last should succeed
- **File Reference:** auth-service/src/main/java/com/enrollment/auth/validation/ValidPassword.java
- PASS

### 2.1.7 Password Obscuring
**Spec Reference:** Checklist item 2.1.7

- **Test:** Password fields are masked
- **Steps:**
  1. Go to login page: http://localhost:3000/login
  2. Go to register page: http://localhost:3000/register
  3. Type in password fields
- **Expected:** Characters appear as dots/asterisks, not plain text

### 2.1.8 Account Lockout Mechanism ⭐
**Spec Reference:** Checklist item 2.1.8

- **Test:** Account locks after failed attempts
- **Steps:**
  1. Register a test account
  2. Attempt login with wrong password 5 times
  3. Try 6th attempt with wrong password
  4. Try with correct password
  5. Wait 15 minutes, try again with correct password
- **Expected:** 
  - Account locked after 5 attempts
  - Lockout lasts 15 minutes
  - Generic error message during lockout
- **File Reference:** auth-service/src/main/java/com/enrollment/auth/service/AuthService.java:163
- PASS

### 2.1.9 Security Questions
**Spec Reference:** Checklist item 2.1.9

- **Test:** Security questions support random answers
- **Steps:**
  1. During registration, check available security questions
  2. Verify questions avoid common answers
- **Expected:** Questions like "favorite book" should be avoided
- **File Reference:** auth-service/src/main/java/com/enrollment/auth/config/SecurityQuestions.java

### 2.1.10 Password Re-use Prevention
**Spec Reference:** Checklist item 2.1.10

- **Test:** Cannot reuse recent passwords
- **Steps:**
  1. Login to existing account
  2. Change password (use /change-password endpoint or frontend)
  3. Immediately try to change back to original password
- **Expected:** Error message about password reuse
- **File Reference:** auth-service/src/main/java/com/enrollment/auth/service/AuthService.java:294-302

### 2.1.11 Password Age Restriction
**Spec Reference:** Checklist item 2.1.11

- **Test:** Password must be 1 day old before change
- **Steps:**
  1. Register new account or change password
  2. Immediately try to change password again
- **Expected:** Error about password being too new (24 hours minimum)
- **File Reference:** auth-service/src/main/java/com/enrollment/auth/service/AuthService.java:284-291

### 2.1.12 Last Login Reporting ⚠️
**Spec Reference:** Checklist item 2.1.12

- **Test:** System reports last login information
- **Steps:**
  1. Login to account
  2. Logout and login again
  3. Check if last login time/location is displayed
- **Expected:** Previous login information shown on dashboard or login response
- **File Reference:** auth-service/src/main/java/com/enrollment/auth/service/AuthService.java:198-225
- PASS
- Caveat: lastLoginTime should be currentLoginTime. Didn't change to avoid issues.

### 2.1.13 Re-authentication for Critical Operations
**Spec Reference:** Checklist item 2.1.13

- **Test:** Password change requires re-authentication
- **URL:** Check password change functionality
- **Steps:**
  1. Login normally
  2. Navigate to password change page
  3. Verify current password is required
- **Expected:** Must enter current password to change it
- **File Reference:** auth-service/src/main/java/com/enrollment/auth/controller/AuthController.java:196-231

---

## Authorization/Access Control Testing (Section 2.2)

### 2.2.1/2.2.2 Access Control ⭐
**Spec Reference:** Checklist items 2.2.1, 2.2.2

- **Test:** Role-based access control
- **Steps:**
  1. Login as student
  2. Try to access admin functions (user management) PASS
  3. Try to access faculty functions (grade submission)
  4. Login as faculty
  5. Try to access admin functions
- **Expected:** Access denied for unauthorized roles, secure failure

### 2.2.3 Business Logic Compliance
**Spec Reference:** Checklist item 2.2.3

- **Test:** Application enforces business rules
- **Examples:**
  1. Student cannot enroll in course at capacity
  2. Faculty can only grade students in their courses
  3. Admin can manage all resources
- **Expected:** Business rules are enforced consistently

---

## Data Validation Testing (Section 2.3)

### 2.3.1 Input Rejection ⚠️
**Spec Reference:** Checklist item 2.3.1

- **Test:** Invalid input is rejected, not sanitized
- **Test Cases:**
  1. SQL injection attempts in email field
  2. XSS scripts in name fields
  3. Invalid email formats
- **Expected:** Input rejected with validation error

### 2.3.2 Data Range Validation
**Spec Reference:** Checklist item 2.3.2

- **Test:** Numeric ranges are validated
- **Examples:**
  1. Course capacity: negative numbers
  2. Grade values: outside 0-100 range
- **Expected:** Range violations rejected

### 2.3.3 Data Length Validation
**Spec Reference:** Checklist item 2.3.3

- **Test:** String length limits enforced
- **Examples:**
  1. Very long names (>255 chars)
  2. Very long email addresses
  3. Very long course descriptions
- **Expected:** Length violations rejected

---

## Error Handling and Logging Testing (Section 2.4)

### 2.4.1 Error Handler Security
**Spec Reference:** Checklist item 2.4.1

- **Test:** No debugging info in error responses
- **Steps:**
  1. Trigger various errors (invalid endpoints, malformed requests)
  2. Check responses for stack traces or debugging info
- **Expected:** Generic error messages only

### 2.4.2 Custom Error Pages
**Spec Reference:** Checklist item 2.4.2

- **Test:** Custom error pages used
- **Steps:**
  1. Access non-existent page: http://localhost:3000/nonexistent
  2. Trigger 500 error
- **Expected:** Custom error pages, not default server pages

### 2.4.3 Security Event Logging
**Spec Reference:** Checklist item 2.4.3

- **Test:** Security events are logged
- **Log Check:**
  1. Check application logs during testing
  2. Look for authentication attempts
  3. Look for authorization failures
- **Expected:** Both success and failure events logged
- **File Reference:** auth-service/src/main/java/com/enrollment/auth/service/SecurityEventLogger.java

### 2.4.4 Log Access Restriction
**Spec Reference:** Checklist item 2.4.4

- **Test:** Only administrators can access logs
- **Steps:**
  1. Login as student/faculty
  2. Try to access log files or log endpoints
- **Expected:** Access denied for non-administrators

### 2.4.5 Input Validation Failure Logging
**Spec Reference:** Checklist item 2.4.5

- **Test:** Validation failures are logged
- **Steps:**
  1. Submit invalid forms
  2. Check logs for validation failure entries
- **Expected:** All validation failures logged

### 2.4.6 Authentication Attempt Logging
**Spec Reference:** Checklist item 2.4.6

- **Test:** All authentication attempts logged
- **Steps:**
  1. Perform successful login
  2. Perform failed login
  3. Check logs for both events
- **Expected:** Both success and failure logged with details
- **File Reference:** auth-service/src/main/java/com/enrollment/auth/service/AuthService.java:210

### 2.4.7 Access Control Failure Logging
**Spec Reference:** Checklist item 2.4.7

- **Test:** Authorization failures are logged
- **Steps:**
  1. Attempt unauthorized access (wrong role)
  2. Check logs for access control failures
- **Expected:** All authorization failures logged

---

## Quick Test Scenarios for Demo

### **5-Minute Demo Test Sequence:**
1. **Authentication**: Register new user, test password complexity
2. **Authorization**: Login and test role-based access
3. **Security**: Show password hashing in database
4. **Account Lockout**: Demonstrate failed login attempts
5. **Error Handling**: Show generic error messages

### **System Startup Verification:**
1. All services running on correct ports (8761, 3001-3004, 3000)
2. Eureka dashboard shows all services registered: http://localhost:8761
3. Frontend accessible: http://localhost:3000
4. MongoDB running and accessible

### **Database Verification Commands:**
```bash
# Connect to MongoDB
mongo auth_service

# Check user collection structure
db.users.findOne()

# Verify password hashing
db.users.find({}, {email: 1, password: 1})
```

---

## Notes for Testers

- **Browser**: Use Chrome/Firefox with Developer Tools open
- **Network Tab**: Monitor API calls and responses
- **Console**: Check for client-side errors
- **Database**: Have MongoDB client ready for verification
- **Logs**: Monitor service logs in terminal windows
- **Timing**: Some tests (lockout, password age) require waiting periods
