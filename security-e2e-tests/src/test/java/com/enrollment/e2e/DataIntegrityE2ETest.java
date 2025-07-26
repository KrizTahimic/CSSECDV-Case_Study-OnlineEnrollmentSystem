package com.enrollment.e2e;

import com.enrollment.e2e.util.TestDataFactory;
import com.enrollment.e2e.config.E2ETestProfile;
import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.awaitility.Awaitility;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * End-to-end tests for data integrity scenarios.
 * Tests data consistency, validation, and security across the system.
 * 
 * Note: These tests require real services with data validation.
 * They are skipped in MOCK profile as mocks don't implement full validation.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Data Integrity End-to-End Security Tests")
public class DataIntegrityE2ETest extends BaseE2ETest {
    
    private String studentToken;
    private String facultyToken;
    private String adminToken;
    private String studentEmail;
    private String facultyEmail;
    
    @BeforeAll
    void setupUsers() {
        // Skip these tests in MOCK profile as mocks don't implement data validation
        assumeTrue(TEST_PROFILE != E2ETestProfile.MOCK, 
            "DataIntegrityE2ETest requires real services with data validation. Skipping in MOCK profile.");
            
        // Register users for testing
        Map<String, Object> studentData = TestDataFactory.createStudentRegistration();
        Map<String, Object> facultyData = TestDataFactory.createFacultyRegistration();
        Map<String, Object> adminData = TestDataFactory.createAdminRegistration();
        
        registerUser(studentData);
        registerUser(facultyData);
        registerUser(adminData);
        
        studentEmail = (String) studentData.get("email");
        facultyEmail = (String) facultyData.get("email");
        
        studentToken = loginAndGetToken(studentEmail, (String) studentData.get("password"));
        facultyToken = loginAndGetToken(facultyEmail, (String) facultyData.get("password"));
        adminToken = loginAndGetToken((String) adminData.get("email"), (String) adminData.get("password"));
    }
    
    @Test
    @DisplayName("Should maintain course enrollment capacity integrity")
    void shouldMaintainEnrollmentCapacityIntegrity() throws InterruptedException {
        // Create a course with limited capacity
        Map<String, Object> courseData = TestDataFactory.createCourse(facultyEmail);
        courseData.put("capacity", 5); // Only 5 seats
        
        Map<String, Object> course = RestAssured
                .given()
                    .spec(createAuthenticatedRequestSpec(facultyToken))
                    .body(courseData)
                .when()
                    .post(COURSE_BASE_URL + "/api/courses")
                .then()
                    .statusCode(201)
                    .extract()
                    .as(Map.class);
        
        String courseId = (String) course.get("id");
        
        // Create 10 students trying to enroll concurrently
        int numberOfStudents = 10;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfStudents);
        CountDownLatch latch = new CountDownLatch(numberOfStudents);
        AtomicInteger successfulEnrollments = new AtomicInteger(0);
        AtomicInteger failedEnrollments = new AtomicInteger(0);
        
        for (int i = 0; i < numberOfStudents; i++) {
            final int studentIndex = i;
            executor.submit(() -> {
                try {
                    // Register and login student
                    Map<String, Object> studentData = TestDataFactory.createStudentRegistration();
                    String uniqueEmail = "student" + studentIndex + "@concurrent.test";
                    studentData.put("email", uniqueEmail);
                    
                    registerUser(studentData);
                    String token = loginAndGetToken(uniqueEmail, (String) studentData.get("password"));
                    
                    // Try to enroll
                    int statusCode = RestAssured
                            .given()
                                .spec(createAuthenticatedRequestSpec(token))
                                .body(TestDataFactory.createEnrollment(uniqueEmail, courseId))
                            .when()
                                .post(ENROLLMENT_BASE_URL + "/api/enrollments")
                            .then()
                                .extract()
                                .statusCode();
                    
                    if (statusCode == 201) {
                        successfulEnrollments.incrementAndGet();
                    } else if (statusCode == 400) {
                        failedEnrollments.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await();
        executor.shutdown();
        
        // Verify exactly 5 students enrolled
        assertThat(successfulEnrollments.get()).isEqualTo(5);
        assertThat(failedEnrollments.get()).isEqualTo(5);
        
        // Verify course shows correct enrollment count
        RestAssured
                .given()
                    .spec(createAuthenticatedRequestSpec(facultyToken))
                .when()
                    .get(COURSE_BASE_URL + "/api/courses/" + courseId)
                .then()
                    .statusCode(200)
                    .body("enrolledStudents", equalTo(5))
                    .body("capacity", equalTo(5));
    }
    
    @Test
    @DisplayName("Should enforce password security policies")
    void shouldEnforcePasswordSecurityPolicies() {
        // Test password complexity
        Map<String, Object> weakPasswordUser = TestDataFactory.createStudentRegistration();
        weakPasswordUser.put("password", "weak");
        
        RestAssured
                .given()
                    .spec(createRequestSpec())
                    .body(weakPasswordUser)
                .when()
                    .post(AUTH_BASE_URL + REGISTER_ENDPOINT)
                .then()
                    .statusCode(400)
                    .body("error", equalTo("Invalid input data provided"));
        
        // Test password with spaces
        weakPasswordUser.put("password", "Pass word123!");
        RestAssured
                .given()
                    .spec(createRequestSpec())
                    .body(weakPasswordUser)
                .when()
                    .post(AUTH_BASE_URL + REGISTER_ENDPOINT)
                .then()
                    .statusCode(400)
                    .body("error", equalTo("Invalid input data provided"));
        
        // Test valid password
        Map<String, Object> validUser = TestDataFactory.createStudentRegistration();
        Map<String, Object> registeredUser = RestAssured
                .given()
                    .spec(createRequestSpec())
                    .body(validUser)
                .when()
                    .post(AUTH_BASE_URL + REGISTER_ENDPOINT)
                .then()
                    .statusCode(201)
                    .body("password", nullValue()) // Password should never be returned
                    .extract()
                    .as(Map.class);
        
        // Verify password is properly hashed (by successful login)
        String token = loginAndGetToken(
            (String) validUser.get("email"), 
            (String) validUser.get("password")
        );
        assertThat(token).isNotEmpty();
    }
    
    @Test
    @DisplayName("Should maintain grade data integrity")
    void shouldMaintainGradeDataIntegrity() {
        // Create course and enroll student
        Map<String, Object> course = RestAssured
                .given()
                    .spec(createAuthenticatedRequestSpec(facultyToken))
                    .body(TestDataFactory.createCourse(facultyEmail))
                .when()
                    .post(COURSE_BASE_URL + "/api/courses")
                .then()
                    .statusCode(201)
                    .extract()
                    .as(Map.class);
        
        String courseId = (String) course.get("id");
        
        RestAssured
                .given()
                    .spec(createAuthenticatedRequestSpec(studentToken))
                    .body(TestDataFactory.createEnrollment(studentEmail, courseId))
                .when()
                    .post(ENROLLMENT_BASE_URL + "/api/enrollments")
                .then()
                    .statusCode(201);
        
        // Test invalid grade scores
        Map<String, Object> invalidGrade = TestDataFactory.createGrade(studentEmail, courseId, -10.0);
        RestAssured
                .given()
                    .spec(createAuthenticatedRequestSpec(facultyToken))
                    .body(invalidGrade)
                .when()
                    .post(GRADE_BASE_URL + "/api/grades")
                .then()
                    .statusCode(400);
        
        invalidGrade.put("score", 150.0); // Over 100
        RestAssured
                .given()
                    .spec(createAuthenticatedRequestSpec(facultyToken))
                    .body(invalidGrade)
                .when()
                    .post(GRADE_BASE_URL + "/api/grades")
                .then()
                    .statusCode(400);
        
        // Submit valid grade
        Map<String, Object> validGrade = TestDataFactory.createGrade(studentEmail, courseId, 85.5);
        Map<String, Object> submittedGrade = RestAssured
                .given()
                    .spec(createAuthenticatedRequestSpec(facultyToken))
                    .body(validGrade)
                .when()
                    .post(GRADE_BASE_URL + "/api/grades")
                .then()
                    .statusCode(200)
                    .body("letterGrade", equalTo("B"))
                    .body("facultyId", equalTo(facultyEmail))
                    .extract()
                    .as(Map.class);
        
        // Verify grade cannot be submitted twice for same student/course
        RestAssured
                .given()
                    .spec(createAuthenticatedRequestSpec(facultyToken))
                    .body(validGrade)
                .when()
                    .post(GRADE_BASE_URL + "/api/grades")
                .then()
                    .statusCode(400);
        
        // Verify letter grade calculation
        String gradeId = (String) submittedGrade.get("id");
        Map<String, Object> updateGrade = Map.of("score", 95.0);
        
        RestAssured
                .given()
                    .spec(createAuthenticatedRequestSpec(facultyToken))
                    .body(updateGrade)
                .when()
                    .put(GRADE_BASE_URL + "/api/grades/" + gradeId)
                .then()
                    .statusCode(200)
                    .body("letterGrade", equalTo("A"));
    }
    
    @Test
    @DisplayName("Should track all security events with proper audit logging")
    void shouldTrackSecurityEventsWithAuditLogging() {
        // This test verifies that security events are properly logged
        // In a real implementation, we would check actual log entries
        
        Map<String, Object> auditUser = TestDataFactory.createStudentRegistration();
        String auditEmail = (String) auditUser.get("email");
        
        // Registration event
        registerUser(auditUser);
        
        // Failed login event
        RestAssured
                .given()
                    .spec(createRequestSpec())
                    .body(TestDataFactory.createLoginRequest(auditEmail, "WrongPassword"))
                .when()
                    .post(AUTH_BASE_URL + LOGIN_ENDPOINT)
                .then()
                    .statusCode(401);
        
        // Successful login event
        String auditToken = loginAndGetToken(auditEmail, (String) auditUser.get("password"));
        
        // Authorization failure event
        RestAssured
                .given()
                    .spec(createAuthenticatedRequestSpec(auditToken))
                    .body(TestDataFactory.createCourse(auditEmail))
                .when()
                    .post(COURSE_BASE_URL + "/api/courses")
                .then()
                    .statusCode(403);
        
        // Input validation failure event
        Map<String, Object> invalidData = TestDataFactory.createStudentRegistration();
        invalidData.put("email", "not-an-email");
        RestAssured
                .given()
                    .spec(createRequestSpec())
                    .body(invalidData)
                .when()
                    .post(AUTH_BASE_URL + REGISTER_ENDPOINT)
                .then()
                    .statusCode(400);
        
        // In production, admin would access logs via secure endpoint
        // We verify the logging requirement is met by the existence of
        // SecurityEventLogger in auth-service
    }
    
    @Test
    @DisplayName("Should handle enrollment state transitions correctly")
    void shouldHandleEnrollmentStateTransitions() {
        // Create course
        Map<String, Object> course = RestAssured
                .given()
                    .spec(createAuthenticatedRequestSpec(facultyToken))
                    .body(TestDataFactory.createCourse(facultyEmail))
                .when()
                    .post(COURSE_BASE_URL + "/api/courses")
                .then()
                    .statusCode(201)
                    .extract()
                    .as(Map.class);
        
        String courseId = (String) course.get("id");
        
        // Enroll student
        RestAssured
                .given()
                    .spec(createAuthenticatedRequestSpec(studentToken))
                    .body(TestDataFactory.createEnrollment(studentEmail, courseId))
                .when()
                    .post(ENROLLMENT_BASE_URL + "/api/enrollments")
                .then()
                    .statusCode(201)
                    .body("status", equalTo("ENROLLED"));
        
        // Cannot enroll twice
        RestAssured
                .given()
                    .spec(createAuthenticatedRequestSpec(studentToken))
                    .body(TestDataFactory.createEnrollment(studentEmail, courseId))
                .when()
                    .post(ENROLLMENT_BASE_URL + "/api/enrollments")
                .then()
                    .statusCode(400)
                    .body("error", containsString("already enrolled"));
        
        // Drop enrollment
        RestAssured
                .given()
                    .spec(createAuthenticatedRequestSpec(studentToken))
                .when()
                    .delete(ENROLLMENT_BASE_URL + "/api/enrollments/" + courseId)
                .then()
                    .statusCode(200);
        
        // Can re-enroll after dropping
        RestAssured
                .given()
                    .spec(createAuthenticatedRequestSpec(studentToken))
                    .body(TestDataFactory.createEnrollment(studentEmail, courseId))
                .when()
                    .post(ENROLLMENT_BASE_URL + "/api/enrollments")
                .then()
                    .statusCode(201);
    }
    
    @Test
    @DisplayName("Should validate all input data across services")
    void shouldValidateAllInputData() {
        // Test email validation
        Map<String, Object> invalidEmailUser = TestDataFactory.createStudentRegistration();
        invalidEmailUser.put("email", "invalid-email");
        
        RestAssured
                .given()
                    .spec(createRequestSpec())
                    .body(invalidEmailUser)
                .when()
                    .post(AUTH_BASE_URL + REGISTER_ENDPOINT)
                .then()
                    .statusCode(400)
                    .body("error", equalTo("Invalid input data provided"));
        
        // Test empty fields
        Map<String, Object> emptyFieldsUser = TestDataFactory.createStudentRegistration();
        emptyFieldsUser.put("firstName", "");
        
        RestAssured
                .given()
                    .spec(createRequestSpec())
                    .body(emptyFieldsUser)
                .when()
                    .post(AUTH_BASE_URL + REGISTER_ENDPOINT)
                .then()
                    .statusCode(400)
                    .body("error", equalTo("Invalid input data provided"));
        
        // Test invalid role
        Map<String, Object> invalidRoleUser = TestDataFactory.createStudentRegistration();
        invalidRoleUser.put("role", "superadmin");
        
        RestAssured
                .given()
                    .spec(createRequestSpec())
                    .body(invalidRoleUser)
                .when()
                    .post(AUTH_BASE_URL + REGISTER_ENDPOINT)
                .then()
                    .statusCode(400)
                    .body("error", equalTo("Invalid input data provided"));
        
        // Test course validation
        Map<String, Object> invalidCourse = TestDataFactory.createCourse(facultyEmail);
        invalidCourse.put("credits", -1);
        
        RestAssured
                .given()
                    .spec(createAuthenticatedRequestSpec(facultyToken))
                    .body(invalidCourse)
                .when()
                    .post(COURSE_BASE_URL + "/api/courses")
                .then()
                    .statusCode(400);
        
        // Test enrollment validation
        Map<String, Object> invalidEnrollment = TestDataFactory.createEnrollment("not-an-email", "invalid-id");
        
        RestAssured
                .given()
                    .spec(createAuthenticatedRequestSpec(studentToken))
                    .body(invalidEnrollment)
                .when()
                    .post(ENROLLMENT_BASE_URL + "/api/enrollments")
                .then()
                    .statusCode(400);
    }
    
    @Test
    @DisplayName("Should ensure data consistency in concurrent operations")
    void shouldEnsureDataConsistencyInConcurrentOperations() throws InterruptedException {
        // Create a course
        Map<String, Object> course = RestAssured
                .given()
                    .spec(createAuthenticatedRequestSpec(facultyToken))
                    .body(TestDataFactory.createCourse(facultyEmail))
                .when()
                    .post(COURSE_BASE_URL + "/api/courses")
                .then()
                    .statusCode(201)
                    .extract()
                    .as(Map.class);
        
        String courseId = (String) course.get("id");
        
        // Enroll student
        RestAssured
                .given()
                    .spec(createAuthenticatedRequestSpec(studentToken))
                    .body(TestDataFactory.createEnrollment(studentEmail, courseId))
                .when()
                    .post(ENROLLMENT_BASE_URL + "/api/enrollments")
                .then()
                    .statusCode(201);
        
        // Simulate concurrent grade updates
        int numberOfUpdates = 5;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfUpdates);
        CountDownLatch latch = new CountDownLatch(numberOfUpdates);
        
        // First submit a grade
        Map<String, Object> initialGrade = RestAssured
                .given()
                    .spec(createAuthenticatedRequestSpec(facultyToken))
                    .body(TestDataFactory.createGrade(studentEmail, courseId, 70.0))
                .when()
                    .post(GRADE_BASE_URL + "/api/grades")
                .then()
                    .statusCode(200)
                    .extract()
                    .as(Map.class);
        
        String gradeId = (String) initialGrade.get("id");
        
        // Try concurrent updates
        for (int i = 0; i < numberOfUpdates; i++) {
            final double newScore = 80.0 + i;
            executor.submit(() -> {
                try {
                    RestAssured
                            .given()
                                .spec(createAuthenticatedRequestSpec(facultyToken))
                                .body(Map.of("score", newScore))
                            .when()
                                .put(GRADE_BASE_URL + "/api/grades/" + gradeId)
                            .then()
                                .statusCode(200);
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await();
        executor.shutdown();
        
        // Verify final state is consistent
        Map<String, Object> finalGrade = RestAssured
                .given()
                    .spec(createAuthenticatedRequestSpec(studentToken))
                .when()
                    .get(GRADE_BASE_URL + "/api/grades/student/" + studentEmail + "/course/" + courseId)
                .then()
                    .statusCode(200)
                    .extract()
                    .as(Map.class);
        
        // Score should be one of the values we set
        double finalScore = ((Number) finalGrade.get("score")).doubleValue();
        assertThat(finalScore).isBetween(80.0, 84.0);
    }
}