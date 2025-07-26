package com.enrollment.e2e;

import com.enrollment.e2e.config.E2ETestProfile;
import com.enrollment.e2e.util.ServiceMockFactory;
import com.enrollment.e2e.util.HealthCheckUtil;
import com.github.tomakehurst.wiremock.WireMockServer;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Base class for all end-to-end security tests.
 * Provides common setup, utilities, and service endpoints.
 */
@SpringBootTest(classes = TestApplication.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
public abstract class BaseE2ETest {
    
    private static final Logger log = LoggerFactory.getLogger(BaseE2ETest.class);
    
    // Test profile
    protected static final E2ETestProfile TEST_PROFILE = E2ETestProfile.getActiveProfile();
    
    // Mock servers (used in MOCK and HYBRID profiles)
    protected static Map<String, WireMockServer> mockServers;
    
    // Network for all containers
    protected static final Network network = Network.newNetwork();
    
    // MongoDB container - using GenericContainer to avoid replica set issues
    @Container
    protected static final GenericContainer<?> mongoDBContainer = new GenericContainer<>(DockerImageName.parse("mongo:7.0"))
            .withNetwork(network)
            .withNetworkAliases("mongodb")
            .withExposedPorts(27017)
            .withEnv("MONGO_INITDB_DATABASE", "test_db")
            .waitingFor(Wait.forLogMessage(".*Waiting for connections.*", 1));
    
    // Redis container  
    @Container
    protected static final GenericContainer<?> redisContainer = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withNetwork(network)
            .withNetworkAliases("redis")
            .withExposedPorts(6379)
            .waitingFor(Wait.forLogMessage(".*Ready to accept connections.*", 1));
    
    // Service ports - matching actual service configuration
    protected static final int AUTH_SERVICE_PORT = 3001;
    protected static final int COURSE_SERVICE_PORT = 3002;
    protected static final int ENROLLMENT_SERVICE_PORT = 3003;
    protected static final int GRADE_SERVICE_PORT = 3004;
    protected static final int EUREKA_PORT = 8761;
    
    // Base URLs - will be updated based on profile
    protected static String AUTH_BASE_URL = "http://localhost:" + AUTH_SERVICE_PORT;
    protected static String COURSE_BASE_URL = "http://localhost:" + COURSE_SERVICE_PORT;
    protected static String ENROLLMENT_BASE_URL = "http://localhost:" + ENROLLMENT_SERVICE_PORT;
    protected static String GRADE_BASE_URL = "http://localhost:" + GRADE_SERVICE_PORT;
    protected static String EUREKA_URL = "http://localhost:" + EUREKA_PORT;
    
    // Common endpoints
    protected static final String REGISTER_ENDPOINT = "/api/auth/register";
    protected static final String LOGIN_ENDPOINT = "/api/auth/login";
    protected static final String REAUTHENTICATE_ENDPOINT = "/api/auth/reauthenticate";
    protected static final String CHANGE_PASSWORD_ENDPOINT = "/api/auth/change-password";
    
    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        // MongoDB configuration from container
        registry.add("spring.data.mongodb.uri", () -> 
            String.format("mongodb://%s:%d/test_db",
                mongoDBContainer.getHost(),
                mongoDBContainer.getMappedPort(27017)));
        
        // Redis configuration from container
        registry.add("spring.redis.host", redisContainer::getHost);
        registry.add("spring.redis.port", () -> redisContainer.getMappedPort(6379));
        
        // Disable Eureka and service discovery for tests
        registry.add("eureka.client.enabled", () -> false);
        registry.add("spring.cloud.discovery.enabled", () -> false);
        
        // JWT configuration
        registry.add("jwt.secret", () -> "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970");
        registry.add("jwt.expiration", () -> 86400000);
        
        // Service-specific configurations
        registry.add("server.port", () -> "0"); // Random port for test context
    }
    
    @BeforeAll
    static void setup() {
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        
        log.info("=== E2E Test Configuration ===");
        log.info("Active Profile: {}", TEST_PROFILE.getProfileName());
        log.info("Description: {}", TEST_PROFILE.getDescription());
        log.info("");
        
        // Setup mock servers if needed
        if (TEST_PROFILE.shouldUseMocks()) {
            log.info("Starting mock services...");
            mockServers = ServiceMockFactory.createFullMockEnvironment();
            log.info("Mock services started successfully");
        }
        
        // For MANUAL mode, verify services are running
        if (TEST_PROFILE == E2ETestProfile.MANUAL) {
            log.info("Manual mode - expecting services to be already running");
            verifyManualServices();
        }
        
        // For INTEGRATION mode, note that services need to be started
        if (TEST_PROFILE == E2ETestProfile.INTEGRATION) {
            log.info("Integration mode - real services required");
            log.info("Note: Tests will only work if services are running or FullServiceE2ETest is used");
            // In integration mode, only FullServiceE2ETest should run
            // Other tests should use mock or hybrid profiles
        }
    }
    
    @AfterAll
    static void teardown() {
        // Cleanup mock servers if they were created
        if (mockServers != null) {
            log.info("Shutting down mock services...");
            ServiceMockFactory.shutdownMockServers(
                mockServers.values().toArray(new WireMockServer[0])
            );
        }
    }
    
    private static void verifyManualServices() {
        // In manual mode, check if services are accessible
        // This is just a warning, not a failure
        boolean authAccessible = HealthCheckUtil.isServiceAccessible("Auth Service", AUTH_BASE_URL + "/actuator/health");
        if (authAccessible) {
            log.info("✓ Auth service is accessible");
        } else {
            log.warn("⚠ Auth service not accessible at {}", AUTH_BASE_URL);
        }
    }
    
    @BeforeEach
    void setupEach() {
        RestAssured.reset();
        
        // Verify connections before each test (except for FullServiceE2ETest which handles its own)
        if (!this.getClass().equals(FullServiceE2ETest.class)) {
            verifyServiceConnections();
        }
    }
    
    /**
     * Creates a request specification with common headers.
     */
    protected RequestSpecification createRequestSpec() {
        return new RequestSpecBuilder()
                .setContentType(ContentType.JSON)
                .setAccept(ContentType.JSON)
                .build();
    }
    
    /**
     * Creates a request specification with JWT authorization.
     */
    protected RequestSpecification createAuthenticatedRequestSpec(String token) {
        return new RequestSpecBuilder()
                .setContentType(ContentType.JSON)
                .setAccept(ContentType.JSON)
                .addHeader("Authorization", "Bearer " + token)
                .build();
    }
    
    /**
     * Registers a user and returns the response.
     */
    protected Map<String, Object> registerUser(Map<String, Object> userData) {
        return RestAssured
                .given()
                    .spec(createRequestSpec())
                    .body(userData)
                .when()
                    .post(AUTH_BASE_URL + REGISTER_ENDPOINT)
                .then()
                    .extract()
                    .as(Map.class);
    }
    
    /**
     * Logs in a user and returns the JWT token.
     */
    protected String loginAndGetToken(String email, String password) {
        log.debug("Attempting login for: {}", email);
        Map<String, Object> response = RestAssured
                .given()
                    .spec(createRequestSpec())
                    .body(Map.of("email", email, "password", password))
                .when()
                    .post(AUTH_BASE_URL + LOGIN_ENDPOINT)
                .then()
                    .statusCode(200)
                    .extract()
                    .as(Map.class);
        
        log.debug("Login response for {}: {}", email, response);
        String token = (String) response.get("token");
        if (token == null) {
            log.error("ERROR: No token in login response for {}: {}", email, response);
        } else {
            log.debug("Token extracted successfully for {}", email);
        }
        return token;
    }
    
    /**
     * Utility method to wait for a specified duration.
     * Useful for testing time-based features like account lockout.
     */
    protected void waitSeconds(int seconds) {
        try {
            Thread.sleep(seconds * 1000L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Test interrupted", e);
        }
    }
    
    /**
     * Verify that required services are accessible before running tests
     */
    private void verifyServiceConnections() {
        if (TEST_PROFILE == E2ETestProfile.INTEGRATION || TEST_PROFILE == E2ETestProfile.MANUAL) {
            // For integration/manual tests, verify each service is accessible
            log.debug("Verifying service connections for {} profile", TEST_PROFILE);
            
            boolean allServicesAccessible = true;
            
            // Skip auth service check for non-auth tests if using mocks partially
            if (!HealthCheckUtil.isServiceAccessible("Auth Service", AUTH_BASE_URL + "/actuator/health")) {
                log.warn("Auth Service is not accessible at {}", AUTH_BASE_URL);
                if (TEST_PROFILE == E2ETestProfile.INTEGRATION) {
                    allServicesAccessible = false;
                }
            }
            
            if (!HealthCheckUtil.isServiceAccessible("Course Service", COURSE_BASE_URL + "/actuator/health")) {
                log.warn("Course Service is not accessible at {}", COURSE_BASE_URL);
                if (TEST_PROFILE == E2ETestProfile.INTEGRATION) {
                    allServicesAccessible = false;
                }
            }
            
            if (!HealthCheckUtil.isServiceAccessible("Enrollment Service", ENROLLMENT_BASE_URL + "/actuator/health")) {
                log.warn("Enrollment Service is not accessible at {}", ENROLLMENT_BASE_URL);
                if (TEST_PROFILE == E2ETestProfile.INTEGRATION) {
                    allServicesAccessible = false;
                }
            }
            
            if (!HealthCheckUtil.isServiceAccessible("Grade Service", GRADE_BASE_URL + "/actuator/health")) {
                log.warn("Grade Service is not accessible at {}", GRADE_BASE_URL);
                if (TEST_PROFILE == E2ETestProfile.INTEGRATION) {
                    allServicesAccessible = false;
                }
            }
            
            if (!allServicesAccessible && TEST_PROFILE == E2ETestProfile.INTEGRATION) {
                throw new IllegalStateException("One or more required services are not accessible. " +
                    "For integration tests, please ensure all services are running or use FullServiceE2ETest.");
            }
            
            if (allServicesAccessible) {
                log.debug("All required services are accessible");
            }
        } else if (TEST_PROFILE.shouldUseMocks()) {
            // For mock tests, just verify WireMock is running
            log.debug("Verifying mock services are accessible");
            // Mock services should be available at this point from BeforeAll
        }
    }
}