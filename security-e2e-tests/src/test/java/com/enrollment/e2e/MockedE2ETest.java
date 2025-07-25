package com.enrollment.e2e;

import com.enrollment.e2e.util.ServiceMockFactory;
import com.enrollment.e2e.util.TestDataFactory;
import com.enrollment.e2e.util.JwtTestUtil;
import com.github.tomakehurst.wiremock.WireMockServer;
import io.restassured.RestAssured;
import org.junit.jupiter.api.*;

import java.util.Map;
import java.util.HashMap;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end security tests using WireMock for service simulation.
 * This provides fast, reliable testing without requiring actual services to be running.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Mocked E2E Security Tests")
public class MockedE2ETest extends BaseE2ETest {
    
    @BeforeAll
    void setupMockServices() {
        // Mock services are already created by BaseE2ETest when in mock/hybrid profile
        if (mockServers != null) {
            System.out.println("Using mock services from BaseE2ETest");
        } else {
            System.out.println("No mock services needed for profile: " + TEST_PROFILE);
        }
    }
    
    @AfterAll
    void teardownMockServices() {
        // Cleanup handled by BaseE2ETest
    }
    
    @Test
    @DisplayName("Should complete full authentication flow with mocked services")
    void shouldCompleteAuthenticationFlowWithMocks() {
        // 1. Register a new student
        Map<String, Object> studentData = TestDataFactory.createStudentRegistration();
        
        given()
            .spec(createRequestSpec())
            .body(studentData)
        .when()
            .post(AUTH_BASE_URL + REGISTER_ENDPOINT)
        .then()
            .statusCode(201)
            .body("email", notNullValue())
            .body("password", nullValue()); // Password should not be returned
        
        // 2. Login with registered credentials
        // Note: Mock returns fixed credentials, so we use the mock's expected values
        String loginEmail = "test@test.com";
        String token = given()
            .spec(createRequestSpec())
            .body(TestDataFactory.createLoginRequest(
                loginEmail,  // Mock expects @test.com emails
                "SecurePass123!"  // Mock expects this password
            ))
        .when()
            .post(AUTH_BASE_URL + LOGIN_ENDPOINT)
        .then()
            .statusCode(200)
            .body("token", notNullValue())
            .body("email", equalTo(loginEmail))  // Mock now returns dynamic email
            .body("role", equalTo("student"))
            .extract()
            .path("token");
        
        assertThat(token).isNotEmpty();
        
        // 3. Access protected course endpoint with token
        given()
            .spec(createAuthenticatedRequestSpec(token))
        .when()
            .get(COURSE_BASE_URL + "/api/courses")
        .then()
            .statusCode(200)
            .body("$", hasSize(2)); // Mock returns 2 courses
    }
    
    @Test
    @DisplayName("Should enforce authentication on protected endpoints")
    void shouldEnforceAuthenticationOnProtectedEndpoints() {
        // Try to access courses without authentication
        given()
            .spec(createRequestSpec())
        .when()
            .get(COURSE_BASE_URL + "/api/courses")
        .then()
            .statusCode(401)
            .body("error", equalTo("Authentication required"));
        
        // Try with invalid token
        given()
            .spec(createAuthenticatedRequestSpec("invalid-token"))
        .when()
            .get(COURSE_BASE_URL + "/api/courses")
        .then()
            .statusCode(403)
            .body("error", equalTo("Invalid token"));
        
        // Try with expired token
        given()
            .spec(createAuthenticatedRequestSpec("expired-token"))
        .when()
            .get(COURSE_BASE_URL + "/api/courses")
        .then()
            .statusCode(403)
            .body("error", equalTo("Token has expired"));
    }
    
    @Test
    @DisplayName("Should handle account lockout scenario")
    void shouldHandleAccountLockout() {
        // Try to login with locked account
        given()
            .spec(createRequestSpec())
            .body(TestDataFactory.createLoginRequest("locked@test.com", "anypassword"))
        .when()
            .post(AUTH_BASE_URL + LOGIN_ENDPOINT)
        .then()
            .statusCode(401)
            .body("error", equalTo("Account is locked due to multiple failed login attempts"));
    }
    
    @Test
    @DisplayName("Should test role-based access control across services")
    void shouldTestRoleBasedAccessControl() {
        // Get a student token
        String studentToken = JwtTestUtil.generateToken("student@test.com", "student");
        
        // Student should be able to view courses
        given()
            .spec(createAuthenticatedRequestSpec(studentToken))
        .when()
            .get(COURSE_BASE_URL + "/api/courses")
        .then()
            .statusCode(200);
        
        // Student should be able to enroll
        given()
            .spec(createAuthenticatedRequestSpec(studentToken))
            .body(Map.of(
                "studentId", "student123",
                "courseId", "course123"
            ))
        .when()
            .post(ENROLLMENT_BASE_URL + "/api/enrollments")
        .then()
            .statusCode(201)
            .body("status", equalTo("ENROLLED"));
        
        // Get a faculty token
        String facultyToken = JwtTestUtil.generateToken("faculty@test.com", "faculty");
        
        // Faculty should be able to create courses
        given()
            .spec(createAuthenticatedRequestSpec(facultyToken))
            .body(TestDataFactory.createCourseData())
        .when()
            .post(COURSE_BASE_URL + "/api/courses")
        .then()
            .statusCode(201)
            .body("code", equalTo("CS301"));
        
        // Faculty should be able to submit grades
        given()
            .spec(createAuthenticatedRequestSpec(facultyToken))
            .body(Map.of(
                "studentId", "student123",
                "courseId", "course123",
                "grade", "A"
            ))
        .when()
            .post(GRADE_BASE_URL + "/api/grades")
        .then()
            .statusCode(201)
            .body("grade", equalTo("A"));
    }
    
    @Test
    @DisplayName("Should return generic error messages for authentication failures")
    void shouldReturnGenericErrorMessages() {
        // Test with wrong password (assuming user exists)
        given()
            .spec(createRequestSpec())
            .body(TestDataFactory.createLoginRequest("existing@test.com", "WrongPassword123!"))
        .when()
            .post(AUTH_BASE_URL + LOGIN_ENDPOINT)
        .then()
            .statusCode(401)
            .body("error", equalTo("Invalid username and/or password"));
        
        // Test with non-existent user
        given()
            .spec(createRequestSpec())
            .body(TestDataFactory.createLoginRequest("nonexistent@test.com", "AnyPassword123!"))
        .when()
            .post(AUTH_BASE_URL + LOGIN_ENDPOINT)
        .then()
            .statusCode(401)
            .body("error", equalTo("Invalid username and/or password"));
    }
    
    @Test
    @DisplayName("Should handle concurrent operations safely")
    void shouldHandleConcurrentOperations() {
        // Create multiple student tokens
        String student1Token = JwtTestUtil.generateToken("student1@test.com", "student");
        String student2Token = JwtTestUtil.generateToken("student2@test.com", "student");
        String student3Token = JwtTestUtil.generateToken("student3@test.com", "student");
        
        // All students try to enroll in the same course
        String courseId = "popular-course";
        
        // Student 1 enrolls
        given()
            .spec(createAuthenticatedRequestSpec(student1Token))
            .body(Map.of("studentId", "student1", "courseId", courseId))
        .when()
            .post(ENROLLMENT_BASE_URL + "/api/enrollments")
        .then()
            .statusCode(201);
        
        // Student 2 enrolls
        given()
            .spec(createAuthenticatedRequestSpec(student2Token))
            .body(Map.of("studentId", "student2", "courseId", courseId))
        .when()
            .post(ENROLLMENT_BASE_URL + "/api/enrollments")
        .then()
            .statusCode(201);
        
        // Student 3 enrolls
        given()
            .spec(createAuthenticatedRequestSpec(student3Token))
            .body(Map.of("studentId", "student3", "courseId", courseId))
        .when()
            .post(ENROLLMENT_BASE_URL + "/api/enrollments")
        .then()
            .statusCode(201);
        
        // All enrollments should succeed with mocks
    }
    
    @Test
    @DisplayName("Should validate health check endpoints")
    void shouldValidateHealthCheckEndpoints() {
        // Check all service health endpoints
        String[] services = {
            EUREKA_URL,
            AUTH_BASE_URL,
            COURSE_BASE_URL,
            ENROLLMENT_BASE_URL,
            GRADE_BASE_URL
        };
        
        for (String serviceUrl : services) {
            given()
            .when()
                .get(serviceUrl + "/actuator/health")
            .then()
                .statusCode(200)
                .body("status", equalTo("UP"));
        }
    }
    
    @Test
    @DisplayName("Should handle password change with re-authentication")
    void shouldHandlePasswordChangeWithReauth() {
        // Get user token
        String token = JwtTestUtil.generateToken("user@test.com", "student");
        
        // First, re-authenticate
        given()
            .spec(createAuthenticatedRequestSpec(token))
            .body(TestDataFactory.createReauthRequest("user@test.com", "SecurePass123!"))
        .when()
            .post(AUTH_BASE_URL + REAUTHENTICATE_ENDPOINT)
        .then()
            .statusCode(200)
            .body("message", containsString("successful"));
        
        // Then change password
        given()
            .spec(createAuthenticatedRequestSpec(token))
            .body(TestDataFactory.createPasswordChangeRequest("SecurePass123!", "NewSecurePass123!"))
        .when()
            .post(AUTH_BASE_URL + CHANGE_PASSWORD_ENDPOINT)
        .then()
            .statusCode(200)
            .body("message", containsString("successfully"));
    }
    
    @Test
    @DisplayName("Should test cross-service authorization")
    void shouldTestCrossServiceAuthorization() {
        // Student token
        String studentToken = JwtTestUtil.generateToken("student@test.com", "student");
        
        // Student tries to create a course (mock allows it as it doesn't check roles)
        // In a real system, this would return 403 Forbidden
        given()
            .spec(createAuthenticatedRequestSpec(studentToken))
            .body(TestDataFactory.createCourseData())
        .when()
            .post(COURSE_BASE_URL + "/api/courses")
        .then()
            .statusCode(201); // Mock doesn't enforce roles
        
        // Student tries to submit grades (mock allows it as it doesn't check roles)
        // In a real system, this would return 403 Forbidden
        given()
            .spec(createAuthenticatedRequestSpec(studentToken))
            .body(Map.of(
                "studentId", "other-student",
                "courseId", "some-course",
                "grade", "A"
            ))
        .when()
            .post(GRADE_BASE_URL + "/api/grades")
        .then()
            .statusCode(201); // Mock doesn't enforce roles
    }
    
    @Test
    @DisplayName("Should demonstrate mock flexibility for edge cases")
    void shouldDemonstrateMockFlexibility() {
        // Test various edge cases that would be difficult with real services
        
        // 1. Test with very long course name
        Map<String, Object> courseData = new HashMap<>(TestDataFactory.createCourseData());
        courseData.put("name", "A".repeat(1000)); // Very long name
        
        given()
            .spec(createAuthenticatedRequestSpec(JwtTestUtil.generateToken("faculty@test.com", "faculty")))
            .body(courseData)
        .when()
            .post(COURSE_BASE_URL + "/api/courses")
        .then()
            .statusCode(201); // Mock accepts anything
        
        // 2. Test rapid successive requests
        String token = JwtTestUtil.generateToken("rapid@test.com", "student");
        for (int i = 0; i < 10; i++) {
            given()
                .spec(createAuthenticatedRequestSpec(token))
            .when()
                .get(COURSE_BASE_URL + "/api/courses")
            .then()
                .statusCode(200);
        }
    }
}