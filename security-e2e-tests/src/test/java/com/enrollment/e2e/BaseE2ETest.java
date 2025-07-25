package com.enrollment.e2e;

import com.enrollment.e2e.config.E2ETestProfile;
import com.enrollment.e2e.util.ServiceMockFactory;
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
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.Map;

/**
 * Base class for all end-to-end security tests.
 * Provides common setup, utilities, and service endpoints.
 */
@SpringBootTest(classes = TestApplication.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
public abstract class BaseE2ETest {
    
    // Test profile
    protected static final E2ETestProfile TEST_PROFILE = E2ETestProfile.getActiveProfile();
    
    // Mock servers (used in MOCK and HYBRID profiles)
    protected static Map<String, WireMockServer> mockServers;
    
    // Network for all containers
    private static final Network network = Network.newNetwork();
    
    // MongoDB container
    @Container
    protected static final MongoDBContainer mongoDBContainer = new MongoDBContainer(DockerImageName.parse("mongo:7.0"))
            .withNetwork(network)
            .withNetworkAliases("mongodb")
            .withExposedPorts(27017);
    
    // Redis container  
    @Container
    protected static final GenericContainer<?> redisContainer = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withNetwork(network)
            .withNetworkAliases("redis")
            .withExposedPorts(6379);
    
    // Service ports - matching actual service configuration
    protected static final int AUTH_SERVICE_PORT = 3001;
    protected static final int COURSE_SERVICE_PORT = 3002;
    protected static final int ENROLLMENT_SERVICE_PORT = 3003;
    protected static final int GRADE_SERVICE_PORT = 3004;
    protected static final int EUREKA_PORT = 8761;
    
    // Base URLs - using standard ports
    protected static final String AUTH_BASE_URL = "http://localhost:" + AUTH_SERVICE_PORT;
    protected static final String COURSE_BASE_URL = "http://localhost:" + COURSE_SERVICE_PORT;
    protected static final String ENROLLMENT_BASE_URL = "http://localhost:" + ENROLLMENT_SERVICE_PORT;
    protected static final String GRADE_BASE_URL = "http://localhost:" + GRADE_SERVICE_PORT;
    protected static final String EUREKA_URL = "http://localhost:" + EUREKA_PORT;
    
    // Common endpoints
    protected static final String REGISTER_ENDPOINT = "/api/auth/register";
    protected static final String LOGIN_ENDPOINT = "/api/auth/login";
    protected static final String REAUTHENTICATE_ENDPOINT = "/api/auth/reauthenticate";
    protected static final String CHANGE_PASSWORD_ENDPOINT = "/api/auth/change-password";
    
    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        // MongoDB configuration from container
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
        
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
        
        System.out.println("=== E2E Test Configuration ===");
        System.out.println("Active Profile: " + TEST_PROFILE.getProfileName());
        System.out.println("Description: " + TEST_PROFILE.getDescription());
        System.out.println();
        
        // Setup mock servers if needed
        if (TEST_PROFILE.shouldUseMocks()) {
            System.out.println("Starting mock services...");
            mockServers = ServiceMockFactory.createFullMockEnvironment();
            System.out.println("Mock services started successfully");
        }
        
        // For MANUAL mode, verify services are running
        if (TEST_PROFILE == E2ETestProfile.MANUAL) {
            System.out.println("Manual mode - expecting services to be already running");
            verifyManualServices();
        }
    }
    
    @AfterAll
    static void teardown() {
        // Cleanup mock servers if they were created
        if (mockServers != null) {
            System.out.println("Shutting down mock services...");
            ServiceMockFactory.shutdownMockServers(
                mockServers.values().toArray(new WireMockServer[0])
            );
        }
    }
    
    private static void verifyManualServices() {
        // In manual mode, check if services are accessible
        // This is just a warning, not a failure
        try {
            RestAssured.get(AUTH_BASE_URL + "/actuator/health");
            System.out.println("✓ Auth service is accessible");
        } catch (Exception e) {
            System.err.println("⚠ Auth service not accessible at " + AUTH_BASE_URL);
        }
    }
    
    @BeforeEach
    void setupEach() {
        RestAssured.reset();
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
        
        return (String) response.get("token");
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
}