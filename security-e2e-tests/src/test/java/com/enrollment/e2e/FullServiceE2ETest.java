package com.enrollment.e2e;

import com.enrollment.e2e.config.E2ETestProfile;
import com.enrollment.e2e.util.ServiceContainerFactory;
import com.enrollment.e2e.util.TestDataFactory;
import io.restassured.RestAssured;
import org.junit.jupiter.api.*;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Full E2E test with real microservices running in Docker containers.
 * This provides the most comprehensive testing but requires Docker images to be built.
 */
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Full Service E2E Tests with Real Containers")
public class FullServiceE2ETest extends BaseE2ETest {
    
    private static Map<String, GenericContainer<?>> serviceContainers;
    
    @BeforeAll
    void setupFullEnvironment() {
        // Only run these tests in integration profile
        assumeTrue(TEST_PROFILE == E2ETestProfile.INTEGRATION,
            "Full service tests only run in integration profile. Use --integration flag.");
        
        try {
            System.out.println("=== Setting up Full Service Environment ===");
            
            // Use the shared network from base class
            // Wait for MongoDB to be ready
            System.out.println("Waiting for MongoDB to be ready...");
            Thread.sleep(5000);
            
            // Get MongoDB and Redis connection info from base containers
            String mongoUri = "mongodb://mongodb:27017";
            String redisHost = "redis";
            
            // Create all service containers on the same network
            serviceContainers = ServiceContainerFactory.createFullServiceEnvironment(
                network, mongoUri, redisHost
            );
            
            // Start containers in order
            System.out.println("Starting Eureka...");
            serviceContainers.get("eureka").start();
            System.out.println("Eureka started, waiting for it to be ready...");
            Thread.sleep(10000); // Give Eureka time to fully initialize
            
            System.out.println("Starting Auth Service...");
            serviceContainers.get("auth").start();
            System.out.println("Waiting for Auth Service to fully initialize...");
            Thread.sleep(20000); // Wait longer for Redis connection and Eureka registration
            
            System.out.println("Starting Course Service...");
            serviceContainers.get("course").start();
            Thread.sleep(5000); // Wait for registration with Eureka
            
            System.out.println("Starting Enrollment Service...");
            serviceContainers.get("enrollment").start();
            Thread.sleep(5000); // Wait for registration with Eureka
            
            System.out.println("Starting Grade Service...");
            serviceContainers.get("grade").start();
            Thread.sleep(10000); // Wait for grade service to register
            
            System.out.println("All services started. Waiting for complete initialization...");
            Thread.sleep(10000); // Additional wait for all services to stabilize
            
            // Update RestAssured to use mapped ports
            updateServiceUrls();
            
            System.out.println("=== Full Service Environment Ready ===");
            
        } catch (Exception e) {
            System.err.println("Failed to start service containers. Make sure Docker images are built:");
            System.err.println("Run: mvn clean package && docker-compose build");
            throw new IllegalStateException("Cannot start service containers", e);
        }
    }
    
    @AfterAll
    void teardownFullEnvironment() {
        if (serviceContainers != null) {
            ServiceContainerFactory.stopContainers(serviceContainers);
        }
    }
    
    private void updateServiceUrls() {
        // Get mapped ports from containers
        int eurekaPort = ServiceContainerFactory.getMappedPort(serviceContainers.get("eureka"), 8761);
        int authPort = ServiceContainerFactory.getMappedPort(serviceContainers.get("auth"), 3001);
        int coursePort = ServiceContainerFactory.getMappedPort(serviceContainers.get("course"), 3002);
        int enrollmentPort = ServiceContainerFactory.getMappedPort(serviceContainers.get("enrollment"), 3003);
        int gradePort = ServiceContainerFactory.getMappedPort(serviceContainers.get("grade"), 3004);
        
        // Update the base URLs with mapped ports
        AUTH_BASE_URL = "http://localhost:" + authPort;
        COURSE_BASE_URL = "http://localhost:" + coursePort;
        ENROLLMENT_BASE_URL = "http://localhost:" + enrollmentPort;
        GRADE_BASE_URL = "http://localhost:" + gradePort;
        EUREKA_URL = "http://localhost:" + eurekaPort;
        
        System.out.println("Service ports mapped:");
        System.out.println("  Eureka: " + eurekaPort + " -> " + EUREKA_URL);
        System.out.println("  Auth: " + authPort + " -> " + AUTH_BASE_URL);
        System.out.println("  Course: " + coursePort + " -> " + COURSE_BASE_URL);
        System.out.println("  Enrollment: " + enrollmentPort + " -> " + ENROLLMENT_BASE_URL);
        System.out.println("  Grade: " + gradePort + " -> " + GRADE_BASE_URL);
    }
    
    @Test
    @DisplayName("Should complete full authentication and authorization flow")
    void shouldCompleteFullAuthAndAuthzFlow() {
        // 1. Register a student
        Map<String, Object> studentData = TestDataFactory.createStudentRegistration();
        
        given()
            .spec(createRequestSpec())
            .body(studentData)
        .when()
            .post(AUTH_BASE_URL + REGISTER_ENDPOINT)
        .then()
            .statusCode(anyOf(is(200), is(201))) // Accept both 200 and 201
            .body("email", equalTo(studentData.get("email")))
            .body("role", equalTo("student"));
        
        // 2. Login and get token
        System.out.println("Attempting login with email: " + studentData.get("email"));
        
        var loginResponse = given()
            .spec(createRequestSpec())
            .body(TestDataFactory.createLoginRequest(
                (String) studentData.get("email"),
                (String) studentData.get("password")
            ))
        .when()
            .post(AUTH_BASE_URL + LOGIN_ENDPOINT)
        .then()
            .log().ifValidationFails()
            .extract()
            .response();
            
        if (loginResponse.statusCode() != 200) {
            System.err.println("Login failed with status: " + loginResponse.statusCode());
            System.err.println("Response: " + loginResponse.asString());
        }
        
        loginResponse.then()
            .statusCode(200)
            .body("token", notNullValue());
            
        String studentToken = loginResponse.path("token");
        
        // 3. Register a faculty member
        Map<String, Object> facultyData = TestDataFactory.createFacultyRegistration();
        
        given()
            .spec(createRequestSpec())
            .body(facultyData)
        .when()
            .post(AUTH_BASE_URL + REGISTER_ENDPOINT)
        .then()
            .statusCode(anyOf(is(200), is(201)));
        
        // 4. Faculty login
        String facultyToken = given()
            .spec(createRequestSpec())
            .body(TestDataFactory.createLoginRequest(
                (String) facultyData.get("email"),
                (String) facultyData.get("password")
            ))
        .when()
            .post(AUTH_BASE_URL + LOGIN_ENDPOINT)
        .then()
            .statusCode(200)
            .extract()
            .path("token");
        
        // 5. Faculty creates a course
        Map<String, Object> courseData = TestDataFactory.createCourseData();
        var courseResponse = given()
            .spec(createAuthenticatedRequestSpec(facultyToken))
            .body(courseData)
        .when()
            .post(COURSE_BASE_URL + "/api/courses")
        .then()
            .statusCode(anyOf(is(200), is(201))) // Accept both if course already exists
            .extract()
            .response();
            
        // Only validate body if we created a new course (201)
        if (courseResponse.statusCode() == 201) {
            courseResponse.then()
                .body("code", equalTo(courseData.get("code")))
                .body("name", equalTo(courseData.get("name")));
        }
        
        String courseId = courseResponse.path("id");
        
        // 6. Student views courses
        given()
            .spec(createAuthenticatedRequestSpec(studentToken))
        .when()
            .get(COURSE_BASE_URL + "/api/courses")
        .then()
            .statusCode(200)
            .body("$", hasSize(greaterThan(0)));
        
        // 7. Student enrolls in course
        Map<String, Object> enrollmentData = Map.of(
            "studentId", studentData.get("email"),
            "courseId", courseId
        );
        
        given()
            .spec(createAuthenticatedRequestSpec(studentToken))
            .body(enrollmentData)
        .when()
            .post(ENROLLMENT_BASE_URL + "/api/enrollments")
        .then()
            .statusCode(anyOf(is(200), is(201))) // Accept both if already enrolled
            .body("studentId", equalTo(studentData.get("email")))
            .body("courseId", equalTo(courseId));
        
        // 8. Faculty submits grade
        Map<String, Object> gradeData = Map.of(
            "studentId", studentData.get("email"),
            "courseId", courseId,
            "grade", "A"
        );
        
        given()
            .spec(createAuthenticatedRequestSpec(facultyToken))
            .body(gradeData)
        .when()
            .post(GRADE_BASE_URL + "/api/grades")
        .then()
            .statusCode(anyOf(is(200), is(201))) // Accept both if grade already exists
            .body("grade", equalTo("A"));
        
        // 9. Student views their grade
        given()
            .spec(createAuthenticatedRequestSpec(studentToken))
        .when()
            .get(GRADE_BASE_URL + "/api/grades/student/" + studentData.get("email"))
        .then()
            .statusCode(200)
            .body("$", hasSize(1))
            .body("[0].grade", equalTo("A"));
    }
    
    @Test
    @DisplayName("Should enforce cross-service security policies")
    void shouldEnforceCrossServiceSecurity() {
        // Create test users
        Map<String, Object> student1 = TestDataFactory.createStudentRegistration();
        Map<String, Object> student2 = TestDataFactory.createStudentRegistration();
        
        // Register both students
        registerUser(student1);
        registerUser(student2);
        
        // Get tokens
        String student1Token = loginAndGetToken(
            (String) student1.get("email"), 
            (String) student1.get("password")
        );
        String student2Token = loginAndGetToken(
            (String) student2.get("email"), 
            (String) student2.get("password")
        );
        
        // Student 1 tries to view Student 2's enrollments (should fail)
        given()
            .spec(createAuthenticatedRequestSpec(student1Token))
        .when()
            .get(ENROLLMENT_BASE_URL + "/api/enrollments/student/" + student2.get("email"))
        .then()
            .statusCode(403)
            .body("error", containsString("access"));
        
        // Student tries to create a course (should fail)
        given()
            .spec(createAuthenticatedRequestSpec(student1Token))
            .body(TestDataFactory.createCourseData())
        .when()
            .post(COURSE_BASE_URL + "/api/courses")
        .then()
            .statusCode(403)
            .body("error", containsString("permission"));
        
        // Student tries to submit grades (should fail)
        given()
            .spec(createAuthenticatedRequestSpec(student1Token))
            .body(Map.of(
                "studentId", student2.get("email"),
                "courseId", "some-course",
                "grade", "F"
            ))
        .when()
            .post(GRADE_BASE_URL + "/api/grades")
        .then()
            .statusCode(403);
    }
    
    @Test
    @DisplayName("Should handle service failures gracefully")
    void shouldHandleServiceFailures() {
        // This test demonstrates resilience when services fail
        
        // Get a valid token
        Map<String, Object> userData = TestDataFactory.createStudentRegistration();
        registerUser(userData);
        String token = loginAndGetToken(
            (String) userData.get("email"), 
            (String) userData.get("password")
        );
        
        // Stop enrollment service to simulate failure
        System.out.println("Simulating enrollment service failure...");
        serviceContainers.get("enrollment").stop();
        
        // Try to enroll (should fail gracefully)
        given()
            .spec(createAuthenticatedRequestSpec(token))
            .body(Map.of(
                "studentId", userData.get("email"),
                "courseId", "test-course"
            ))
        .when()
            .post(ENROLLMENT_BASE_URL + "/api/enrollments")
        .then()
            .statusCode(anyOf(is(500), is(503)))
            .body("error", containsString("service"));
        
        // Restart enrollment service
        System.out.println("Restarting enrollment service...");
        serviceContainers.get("enrollment").start();
        
        // Wait for service to be healthy
        waitSeconds(10);
        
        // Should work now
        given()
            .spec(createAuthenticatedRequestSpec(token))
        .when()
            .get(ENROLLMENT_BASE_URL + "/api/enrollments/student/" + userData.get("email"))
        .then()
            .statusCode(200);
    }
    
    @Test
    @DisplayName("Should maintain data consistency across services")
    void shouldMaintainDataConsistency() {
        // Test that data remains consistent across service boundaries
        
        // Create faculty and course
        Map<String, Object> facultyData = TestDataFactory.createFacultyRegistration();
        registerUser(facultyData);
        String facultyToken = loginAndGetToken(
            (String) facultyData.get("email"),
            (String) facultyData.get("password")
        );
        
        // Create course with specific capacity
        Map<String, Object> courseData = TestDataFactory.createCourseData();
        courseData.put("capacity", 2); // Only 2 spots
        
        String courseId = given()
            .spec(createAuthenticatedRequestSpec(facultyToken))
            .body(courseData)
        .when()
            .post(COURSE_BASE_URL + "/api/courses")
        .then()
            .statusCode(anyOf(is(200), is(201))) // Accept both if course already exists
            .extract()
            .path("id");
        
        // Create 3 students
        String[] studentTokens = new String[3];
        String[] studentEmails = new String[3];
        
        for (int i = 0; i < 3; i++) {
            Map<String, Object> studentData = TestDataFactory.createStudentRegistration();
            registerUser(studentData);
            studentEmails[i] = (String) studentData.get("email");
            studentTokens[i] = loginAndGetToken(studentEmails[i], (String) studentData.get("password"));
        }
        
        // First two students should enroll successfully
        for (int i = 0; i < 2; i++) {
            given()
                .spec(createAuthenticatedRequestSpec(studentTokens[i]))
                .body(Map.of(
                    "studentId", studentEmails[i],
                    "courseId", courseId
                ))
            .when()
                .post(ENROLLMENT_BASE_URL + "/api/enrollments")
            .then()
                .statusCode(201);
        }
        
        // Third student should fail (course full)
        given()
            .spec(createAuthenticatedRequestSpec(studentTokens[2]))
            .body(Map.of(
                "studentId", studentEmails[2],
                "courseId", courseId
            ))
        .when()
            .post(ENROLLMENT_BASE_URL + "/api/enrollments")
        .then()
            .statusCode(400)
            .body("error", containsString("full"));
        
        // Verify course shows correct enrollment count
        given()
            .spec(createAuthenticatedRequestSpec(facultyToken))
        .when()
            .get(COURSE_BASE_URL + "/api/courses/" + courseId)
        .then()
            .statusCode(200)
            .body("enrolled", equalTo(2))
            .body("capacity", equalTo(2));
    }
    
    @Test
    @DisplayName("Should verify service discovery integration")
    void shouldVerifyServiceDiscovery() {
        // Check that all services are registered with Eureka
        
        given()
        .when()
            .get(EUREKA_URL + "/eureka/apps")
        .then()
            .statusCode(200)
            .body("applications.application", hasSize(greaterThanOrEqualTo(4)))
            .body("applications.application.name", hasItems(
                "AUTH-SERVICE",
                "COURSE-SERVICE", 
                "ENROLLMENT-SERVICE",
                "GRADE-SERVICE"
            ));
    }
}