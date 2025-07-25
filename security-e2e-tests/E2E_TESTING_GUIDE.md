# End-to-End Security Testing Guide

## Overview

This guide explains the comprehensive E2E security testing framework for the Online Enrollment System. The framework supports multiple testing profiles to balance speed, coverage, and resource usage.

## Testing Profiles

### 1. Mock Profile (`--mock`)
- **Description**: Uses WireMock to simulate all microservices
- **Speed**: Fast (~10-30 seconds)
- **Resources**: Minimal (no containers)
- **Use Case**: Quick security validation, CI/CD pipelines
- **Coverage**: API contracts and security flows

### 2. Integration Profile (`--integration`)
- **Description**: Runs all services in Docker containers
- **Speed**: Slow (~2-3 minutes startup)
- **Resources**: High (5+ containers)
- **Use Case**: Full system validation, release testing
- **Coverage**: Complete end-to-end scenarios

### 3. Hybrid Profile (`--hybrid`) [Default]
- **Description**: MongoDB/Redis in containers + WireMock services
- **Speed**: Medium (~30-60 seconds)
- **Resources**: Moderate (2 containers)
- **Use Case**: Development testing, feature validation
- **Coverage**: Data persistence + API flows

### 4. Manual Profile (`--manual`)
- **Description**: Expects services to be manually started
- **Speed**: Depends on existing services
- **Resources**: External services required
- **Use Case**: Local development, debugging
- **Coverage**: Testing against local services

## Quick Start

### Running Tests

```bash
# Quick mock tests
./run-e2e-tests.sh --mock

# Full integration tests (requires Docker)
./run-e2e-tests.sh --integration

# Default hybrid mode
./run-e2e-tests.sh

# Specific test with profile
./run-e2e-tests.sh --hybrid --test AuthenticationE2ETest

# Manual mode (services already running)
./run-e2e-tests.sh --manual
```

### Prerequisites

#### For Mock Profile
- Java 17+
- Maven 3.6+
- No additional requirements

#### For Integration Profile
- Docker Desktop installed and running
- Built service images: `mvn clean package && docker-compose build`
- At least 8GB RAM available
- Ports 3001-3004, 8761, 27017, 6379 available

#### For Hybrid Profile
- Docker Desktop installed and running
- Ports 27017, 6379 available for MongoDB/Redis

#### For Manual Profile
- All services running locally:
  - MongoDB on 27017
  - Redis on 6379
  - Eureka on 8761
  - Auth Service on 3001
  - Course Service on 3002
  - Enrollment Service on 3003
  - Grade Service on 3004

## Test Structure

### Test Classes

1. **MockedE2ETest**
   - Tests using WireMock mocks
   - Fast, predictable responses
   - Good for security flow validation

2. **AuthenticationE2ETest**
   - Registration and login flows
   - Password policies
   - Account lockout
   - Re-authentication

3. **AuthorizationE2ETest**
   - Role-based access control
   - Cross-service authorization
   - Business logic enforcement

4. **SecurityAttackE2ETest**
   - Brute force protection
   - Token manipulation
   - SQL injection attempts
   - Privilege escalation

5. **DataIntegrityE2ETest**
   - Concurrent operations
   - Data validation
   - Audit logging
   - Transaction consistency

6. **FullServiceE2ETest**
   - Complete integration scenarios
   - Service discovery validation
   - Failure handling
   - Data consistency

## Security Test Coverage

### Authentication Tests
- ✅ User registration with validation
- ✅ Login with JWT generation
- ✅ Password complexity enforcement
- ✅ Account lockout after failed attempts
- ✅ Last login tracking
- ✅ Re-authentication for sensitive operations
- ✅ Generic error messages

### Authorization Tests
- ✅ Role-based endpoint access
- ✅ Student permissions
- ✅ Faculty permissions
- ✅ Admin permissions
- ✅ Cross-service token validation
- ✅ Business logic authorization

### Security Attack Tests
- ✅ Brute force protection
- ✅ Token expiration handling
- ✅ Invalid token rejection
- ✅ Privilege escalation prevention
- ✅ Input validation
- ✅ Rate limiting simulation

### Data Integrity Tests
- ✅ Concurrent enrollment handling
- ✅ Course capacity enforcement
- ✅ Grade submission validation
- ✅ Audit trail verification
- ✅ Data consistency checks

## Configuration

### Environment Variables

```bash
# Set test profile
export E2E_TEST_PROFILE=mock

# Or use system property
mvn test -De2e.test.profile=integration
```

### Custom Service URLs

For manual mode with non-standard ports:
```java
// In your test setup
System.setProperty("auth.service.url", "http://localhost:8081");
System.setProperty("course.service.url", "http://localhost:8082");
```

## Troubleshooting

### Common Issues

1. **"Cannot connect to Docker daemon"**
   - Ensure Docker Desktop is running
   - Check Docker permissions
   - Try: `docker ps` to verify

2. **"Port already in use"**
   - Mock/Manual modes conflict
   - Stop local services or use different profile
   - Check: `lsof -i :3001` (Mac/Linux)

3. **"Service not healthy"**
   - Integration mode startup timeout
   - Check service logs: `docker logs <container>`
   - Increase timeout in ServiceContainerFactory

4. **"Test timeout"**
   - Network issues between containers
   - Check Docker network: `docker network ls`
   - Verify service discovery registration

### Debugging Tips

1. **Enable detailed logging**:
   ```bash
   export MAVEN_OPTS="-Dlogging.level.com.enrollment=DEBUG"
   ./run-e2e-tests.sh
   ```

2. **View container logs**:
   ```bash
   docker-compose -f docker-compose.test.yml logs -f auth-service
   ```

3. **Interactive test mode**:
   ```bash
   mvn test -Dtest=AuthenticationE2ETest#shouldCompleteFullAuthenticationFlow -X
   ```

## Best Practices

### When to Use Each Profile

1. **Use Mock Profile When**:
   - Developing new security tests
   - Running in CI/CD pipelines
   - Testing security flows without data persistence
   - Quick validation during development

2. **Use Integration Profile When**:
   - Validating complete system behavior
   - Testing service interactions
   - Verifying data consistency
   - Release candidate testing

3. **Use Hybrid Profile When**:
   - Need data persistence but fast execution
   - Testing database-related security
   - Default development workflow
   - Balancing speed and coverage

4. **Use Manual Profile When**:
   - Debugging specific service issues
   - Testing against modified local services
   - Avoiding container overhead
   - Using custom service configurations

### Writing New Tests

1. **Extend BaseE2ETest**:
   ```java
   public class MySecurityTest extends BaseE2ETest {
       // Inherits profile support and utilities
   }
   ```

2. **Use TestDataFactory**:
   ```java
   Map<String, Object> userData = TestDataFactory.createStudentRegistration();
   ```

3. **Check profile when needed**:
   ```java
   if (TEST_PROFILE.shouldUseMocks()) {
       // Mock-specific setup
   }
   ```

4. **Use appropriate timeouts**:
   ```java
   await()
       .atMost(30, TimeUnit.SECONDS)
       .until(() -> serviceIsHealthy());
   ```

## Performance Optimization

### Mock Profile
- Startup: ~5 seconds
- Test execution: ~10-20 seconds
- Total: ~30 seconds

### Hybrid Profile
- Container startup: ~20 seconds
- Test execution: ~20-30 seconds
- Total: ~60 seconds

### Integration Profile
- Container startup: ~90 seconds
- Test execution: ~30-60 seconds
- Total: ~2-3 minutes

### Tips for Faster Tests
1. Run specific tests: `--test AuthenticationE2ETest`
2. Use mock profile for development
3. Parallelize independent tests
4. Cache Docker images locally
5. Increase container resources

## Continuous Integration

### GitHub Actions Example
```yaml
- name: Run Security E2E Tests
  run: |
    ./run-e2e-tests.sh --mock
    
- name: Run Integration Tests
  if: github.ref == 'refs/heads/main'
  run: |
    docker-compose build
    ./run-e2e-tests.sh --integration
```

### Jenkins Pipeline Example
```groovy
stage('E2E Security Tests') {
    steps {
        sh './run-e2e-tests.sh --hybrid'
    }
}
```

## Contributing

### Adding New Security Tests
1. Identify security scenario
2. Choose appropriate test class
3. Use existing utilities (JwtTestUtil, TestDataFactory)
4. Test in multiple profiles
5. Document expected behavior

### Extending the Framework
1. Add new profiles in E2ETestProfile
2. Update ServiceMockFactory for new endpoints
3. Enhance ServiceContainerFactory for new services
4. Update run-e2e-tests.sh for new options

## Summary

The E2E security testing framework provides comprehensive coverage of the Online Enrollment System's security requirements. By supporting multiple profiles, it enables:

- **Fast feedback** during development (mock)
- **Complete validation** for releases (integration)
- **Balanced testing** for daily work (hybrid)
- **Flexible debugging** options (manual)

Choose the appropriate profile based on your testing needs and constraints. When in doubt, use the default hybrid profile for a good balance of speed and coverage.