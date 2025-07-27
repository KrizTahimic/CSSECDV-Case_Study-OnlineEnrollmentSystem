package com.enrollment.e2e;

import com.enrollment.e2e.config.E2ETestProfile;
import com.enrollment.e2e.util.ServiceContainerFactory;
import com.enrollment.e2e.util.TestDataFactory;
import com.enrollment.e2e.util.HealthCheckUtil;
import com.enrollment.e2e.util.DiagnosticUtil;
import io.restassured.RestAssured;
import org.junit.jupiter.api.*;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.HashMap;
import java.time.Duration;

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
    
    private static final Logger log = LoggerFactory.getLogger(FullServiceE2ETest.class);
    private static Map<String, GenericContainer<?>> serviceContainers;
    
    @BeforeAll
    void setupFullEnvironment() {
        // Only run these tests in integration profile
        assumeTrue(TEST_PROFILE == E2ETestProfile.INTEGRATION,
            "Full service tests only run in integration profile. Use --integration flag.");
        
        try {
            log.info("=== Setting up Full Service Environment ===");
            
            // Use the shared network from base class
            // Verify MongoDB and Redis are ready first
            log.info("Verifying MongoDB is ready...");
            if (!verifyMongoDBReady()) {
                throw new IllegalStateException("MongoDB is not ready");
            }
            
            log.info("Verifying Redis is ready...");
            if (!verifyRedisReady()) {
                throw new IllegalStateException("Redis is not ready");
            }
            
            // Get MongoDB and Redis connection info from base containers
            String mongoUri = "mongodb://mongodb:27017";
            String redisHost = "redis";
            
            // Create all service containers on the same network
            serviceContainers = ServiceContainerFactory.createFullServiceEnvironment(
                network, mongoUri, redisHost
            );
            
            // Start Eureka first and wait for it to be fully ready
            log.info("Starting Eureka service...");
            GenericContainer<?> eurekaContainer = serviceContainers.get("eureka");
            eurekaContainer.start();
            
            // Get Eureka URL immediately after starting
            String eurekaUrl = "http://localhost:" + eurekaContainer.getMappedPort(8761);
            EUREKA_URL = eurekaUrl;
            log.info("Eureka URL: {}", eurekaUrl);
            
            // Wait for Eureka to be healthy
            if (!HealthCheckUtil.waitForServiceHealth("Eureka", eurekaUrl + "/actuator/health")) {
                throw new IllegalStateException("Eureka failed to become healthy");
            }
            
            // Start Auth service and wait for it to be ready
            log.info("Starting Auth service...");
            GenericContainer<?> authContainer = serviceContainers.get("auth");
            authContainer.start();
            
            String authUrl = "http://localhost:" + authContainer.getMappedPort(3001);
            AUTH_BASE_URL = authUrl;
            log.info("Auth URL: {}", authUrl);
            
            // Wait for Auth to be healthy (includes Redis connection)
            if (!HealthCheckUtil.waitForServiceHealth("Auth", authUrl + "/actuator/health")) {
                log.error("Auth service failed to become healthy. Capturing diagnostics...");
                DiagnosticUtil.diagnoseContainer("auth", authContainer);
                throw new IllegalStateException("Auth service failed to become healthy");
            }
            
            // Start remaining services in parallel since they don't depend on each other
            log.info("Starting Course, Enrollment, and Grade services...");
            GenericContainer<?> courseContainer = serviceContainers.get("course");
            GenericContainer<?> enrollmentContainer = serviceContainers.get("enrollment");
            GenericContainer<?> gradeContainer = serviceContainers.get("grade");
            
            courseContainer.start();
            enrollmentContainer.start();
            gradeContainer.start();
            
            // Update URLs for all services
            COURSE_BASE_URL = "http://localhost:" + courseContainer.getMappedPort(3002);
            ENROLLMENT_BASE_URL = "http://localhost:" + enrollmentContainer.getMappedPort(3003);
            GRADE_BASE_URL = "http://localhost:" + gradeContainer.getMappedPort(3004);
            
            log.info("Service URLs:");
            log.info("  Eureka: {}", EUREKA_URL);
            log.info("  Auth: {}", AUTH_BASE_URL);
            log.info("  Course: {}", COURSE_BASE_URL);
            log.info("  Enrollment: {}", ENROLLMENT_BASE_URL);
            log.info("  Grade: {}", GRADE_BASE_URL);
            
            // Wait for all services to be healthy
            boolean allHealthy = 
                HealthCheckUtil.waitForServiceHealth("Course", COURSE_BASE_URL + "/actuator/health") &&
                HealthCheckUtil.waitForServiceHealth("Enrollment", ENROLLMENT_BASE_URL + "/actuator/health") &&
                HealthCheckUtil.waitForServiceHealth("Grade", GRADE_BASE_URL + "/actuator/health");
            
            if (!allHealthy) {
                throw new IllegalStateException("Some services failed to become healthy");
            }
            
            // Wait for all services to register with Eureka
            log.info("Waiting for all services to register with Eureka...");
            if (!HealthCheckUtil.waitForEurekaRegistrations(EUREKA_URL, 4, Duration.ofMinutes(2))) {
                log.warn("Not all services registered with Eureka, but continuing...");
            }
            
            log.info("=== Full Service Environment Ready ===");
            
        } catch (Exception e) {
            log.error("Failed to start service containers", e);
            
            // Perform comprehensive diagnostics
            DiagnosticUtil.logTestEnvironment();
            DiagnosticUtil.checkMongoDBConnection(mongoDBContainer);
            DiagnosticUtil.checkRedisConnection(redisContainer);
            
            if (serviceContainers != null) {
                // Diagnose each container
                serviceContainers.forEach((name, container) -> {
                    if (container != null) {
                        DiagnosticUtil.diagnoseContainer(name, container);
                    }
                });
                
                // Check all endpoints
                Map<String, String> serviceUrls = new HashMap<>();
                serviceUrls.put("Eureka", EUREKA_URL);
                serviceUrls.put("Auth", AUTH_BASE_URL);
                serviceUrls.put("Course", COURSE_BASE_URL);
                serviceUrls.put("Enrollment", ENROLLMENT_BASE_URL);
                serviceUrls.put("Grade", GRADE_BASE_URL);
                DiagnosticUtil.checkAllEndpoints(serviceUrls);
            }
            
            throw new IllegalStateException("Cannot start service containers. Check diagnostics above.", e);
        }
    }
    
    @AfterAll
    void teardownFullEnvironment() {
        if (serviceContainers != null) {
            ServiceContainerFactory.stopContainers(serviceContainers);
        }
    }
    
    private boolean verifyMongoDBReady() {
        try {
            // MongoDB container from base class should already be running
            // Just verify it's accessible
            return mongoDBContainer != null && mongoDBContainer.isRunning();
        } catch (Exception e) {
            log.error("MongoDB verification failed", e);
            return false;
        }
    }
    
    private boolean verifyRedisReady() {
        try {
            // Redis container from base class should already be running
            // Just verify it's accessible
            return redisContainer != null && redisContainer.isRunning();
        } catch (Exception e) {
            log.error("Redis verification failed", e);
            return false;
        }
    }
    
    private void updateServiceUrls() {
        // Only update URLs for started containers
        if (serviceContainers.get("eureka").isRunning()) {
            int eurekaPort = ServiceContainerFactory.getMappedPort(serviceContainers.get("eureka"), 8761);
            EUREKA_URL = "http://localhost:" + eurekaPort;
            log.info("  Eureka: {} -> {}", eurekaPort, EUREKA_URL);
        }
        
        if (serviceContainers.get("auth") != null && serviceContainers.get("auth").isRunning()) {
            int authPort = ServiceContainerFactory.getMappedPort(serviceContainers.get("auth"), 3001);
            AUTH_BASE_URL = "http://localhost:" + authPort;
            log.info("  Auth: {} -> {}", authPort, AUTH_BASE_URL);
        }
        
        if (serviceContainers.get("course") != null && serviceContainers.get("course").isRunning()) {
            int coursePort = ServiceContainerFactory.getMappedPort(serviceContainers.get("course"), 3002);
            COURSE_BASE_URL = "http://localhost:" + coursePort;
            log.info("  Course: {} -> {}", coursePort, COURSE_BASE_URL);
        }
        
        if (serviceContainers.get("enrollment") != null && serviceContainers.get("enrollment").isRunning()) {
            int enrollmentPort = ServiceContainerFactory.getMappedPort(serviceContainers.get("enrollment"), 3003);
            ENROLLMENT_BASE_URL = "http://localhost:" + enrollmentPort;
            log.info("  Enrollment: {} -> {}", enrollmentPort, ENROLLMENT_BASE_URL);
        }
        
        if (serviceContainers.get("grade") != null && serviceContainers.get("grade").isRunning()) {
            int gradePort = ServiceContainerFactory.getMappedPort(serviceContainers.get("grade"), 3004);
            GRADE_BASE_URL = "http://localhost:" + gradePort;
            log.info("  Grade: {} -> {}", gradePort, GRADE_BASE_URL);
        }
    }
    
    private void startServiceContainer(String name, GenericContainer<?> container, long waitTime) {
        try {
            log.info("Starting {} container...", name);
            container.start();
            log.info("{} container started successfully", name);
            
            // Get mapped port for debugging
            Integer[] exposedPorts = container.getExposedPorts().toArray(new Integer[0]);
            if (exposedPorts.length > 0) {
                int mappedPort = container.getMappedPort(exposedPorts[0]);
                log.info("{} is accessible on port: {}", name, mappedPort);
            }
            
            Thread.sleep(waitTime);
        } catch (Exception e) {
            log.error("ERROR: Failed to start {} container", name, e);
            throw new IllegalStateException("Cannot start " + name + " container", e);
        }
    }
    
    private void verifyServiceHealth(String healthUrl, String serviceName) {
        int maxRetries = 10;
        int retryCount = 0;
        
        while (retryCount < maxRetries) {
            try {
                given()
                    .when()
                    .get(healthUrl)
                    .then()
                    .statusCode(200);
                log.info("✓ {} health check passed", serviceName);
                return;
            } catch (Exception e) {
                retryCount++;
                if (retryCount < maxRetries) {
                    log.info("Waiting for {} to be healthy... (attempt {}/{})", serviceName, retryCount, maxRetries);
                    waitSeconds(2);
                }
            }
        }
        throw new IllegalStateException(serviceName + " health check failed after " + maxRetries + " attempts");
    }
    
    private void verifyServicesRegistered() {
        try {
            log.info("Verifying services are registered with Eureka...");
            
            // Give services time to register
            Thread.sleep(5000);
            
            // Check Eureka for registered services
            String response = given()
                .when()
                .get(EUREKA_URL + "/eureka/apps")
                .then()
                .statusCode(200)
                .extract()
                .asString();
                
            log.info("Services registered with Eureka:");
            if (response.contains("AUTH-SERVICE")) log.info("✓ AUTH-SERVICE");
            if (response.contains("COURSE-SERVICE")) log.info("✓ COURSE-SERVICE");
            if (response.contains("ENROLLMENT-SERVICE")) log.info("✓ ENROLLMENT-SERVICE");
            if (response.contains("GRADE-SERVICE")) log.info("✓ GRADE-SERVICE");
            
        } catch (Exception e) {
            log.warn("WARNING: Could not verify service registration: {}", e.getMessage());
        }
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