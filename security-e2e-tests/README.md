# Security End-to-End Tests

This module contains comprehensive end-to-end security tests for the Online Enrollment System microservices.

## Overview

The security e2e tests validate that security controls work correctly across the entire distributed system, ensuring:
- Authentication flows are secure
- Authorization is properly enforced across service boundaries
- System is resilient against common attacks
- Data integrity is maintained

## Test Categories

### 1. Authentication Tests (`AuthenticationE2ETest`)
- Complete registration and login flows
- Account lockout after failed attempts
- Password change with re-authentication
- Last login tracking
- Password complexity validation
- Generic error messages

### 2. Authorization Tests (`AuthorizationE2ETest`)
- Role-based access control for courses, enrollments, and grades
- Student enrollment workflow
- Faculty grade submission
- Cross-service authorization consistency
- Business logic flow enforcement

### 3. Security Attack Tests (`SecurityAttackE2ETest`)
- Brute force protection
- JWT token manipulation detection
- Privilege escalation prevention
- Input injection handling
- CORS protection
- Service availability under attack
- Error message security

### 4. Data Integrity Tests (`DataIntegrityE2ETest`)
- Concurrent enrollment capacity handling
- Password security policy enforcement
- Grade data validation
- Security event audit logging
- Enrollment state transitions
- Input validation across all services

## Prerequisites

### Required Services
The tests require the following services to be running:
- MongoDB (port 27017)
- Redis (port 6379)
- Auth Service (port 3001)
- Course Service (port 3002)
- Enrollment Service (port 3003)
- Grade Service (port 3004)

### Using Test Containers
The tests use Testcontainers to automatically start MongoDB and Redis:
```java
@TestConfiguration
@Testcontainers
public class TestContainersConfig {
    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:7.0");
    
    @Container
    static GenericContainer<?> redisContainer = new GenericContainer<>("redis:7-alpine");
}
```

## Running the Tests

### Run All E2E Tests
```bash
cd security-e2e-tests
mvn test
```

### Run Specific Test Class
```bash
mvn test -Dtest=AuthenticationE2ETest
mvn test -Dtest=AuthorizationE2ETest
mvn test -Dtest=SecurityAttackE2ETest
mvn test -Dtest=DataIntegrityE2ETest
```

### Run with Detailed Output
```bash
mvn test -Dtest.output=true
```

## Test Configuration

Configuration is in `src/test/resources/application-test.yml`:
- JWT secret and expiration
- Service URLs
- Test timeouts
- Logging levels

## Key Test Scenarios

### Authentication Flow
```java
@Test
void shouldCompleteFullAuthenticationFlow() {
    // 1. Register new user
    // 2. Login and receive JWT
    // 3. Verify last login tracking
    // 4. Access protected resources
}
```

### Authorization Chain
```java
@Test
void shouldCompleteStudentEnrollmentWorkflow() {
    // 1. Faculty creates course
    // 2. Student enrolls
    // 3. Faculty submits grade
    // 4. Student views grade
    // 5. Verify authorization at each step
}
```

### Security Attack
```java
@Test
void shouldProtectAgainstBruteForce() {
    // 1. Multiple failed login attempts
    // 2. Account lockout triggered
    // 3. Legitimate login blocked
    // 4. Service remains available
}
```

### Data Integrity
```java
@Test
void shouldMaintainEnrollmentCapacityIntegrity() {
    // 1. Create course with limited capacity
    // 2. Concurrent enrollment attempts
    // 3. Verify exactly N students enrolled
    // 4. Verify course capacity enforced
}
```

## CSSECDV Requirements Coverage

The tests validate all major CSSECDV requirements:

- **2.1.x Authentication**: Password policies, lockout, last login, re-authentication
- **2.2.x Authorization**: Role-based access, fail securely, business logic
- **2.3.x Data Validation**: Input rejection, range/length validation
- **2.4.x Error Handling**: Generic errors, security logging

## Test Utilities

### JWT Test Utility
```java
public class JwtTestUtil {
    // Generate valid tokens for testing
    public static String generateToken(String email, String role)
    
    // Generate expired tokens
    public static String generateExpiredToken(String email, String role)
    
    // Generate invalid tokens
    public static String generateInvalidToken()
}
```

### Test Data Factory
```java
public class TestDataFactory {
    // Create test users
    public static Map<String, Object> createStudentRegistration()
    public static Map<String, Object> createFacultyRegistration()
    
    // Create test data
    public static Map<String, Object> createCourse(String facultyEmail)
    public static Map<String, Object> createEnrollment(String studentEmail, String courseId)
    public static Map<String, Object> createGrade(String studentEmail, String courseId, double score)
}
```

## Troubleshooting

### Tests Fail with Connection Refused
- Ensure all microservices are running
- Check service ports match configuration
- Verify MongoDB and Redis are accessible

### Testcontainers Issues
- Ensure Docker is running
- Check Docker has sufficient resources
- Try `docker system prune` if containers fail to start

### Timeout Issues
- Increase test timeouts in application-test.yml
- Check service startup time
- Ensure adequate system resources

## Best Practices

1. **Test Independence**: Each test should be independent and not rely on others
2. **Data Cleanup**: Tests create unique data to avoid conflicts
3. **Concurrent Testing**: Use proper synchronization for concurrent tests
4. **Error Verification**: Always verify both success and failure cases
5. **Security Focus**: Tests should validate security controls, not just functionality

## Contributing

When adding new e2e tests:
1. Follow existing test patterns
2. Use TestDataFactory for consistent data
3. Document test scenarios clearly
4. Ensure CSSECDV requirement coverage
5. Add to appropriate test class or create new one