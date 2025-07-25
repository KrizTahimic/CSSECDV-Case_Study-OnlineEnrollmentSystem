package com.enrollment.e2e.util;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.extension.responsetemplating.ResponseTemplateTransformer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

/**
 * Factory for creating WireMock servers that simulate microservices.
 * Provides consistent mock responses for security testing.
 */
public class ServiceMockFactory {
    
    private static final String JWT_SECRET = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    
    /**
     * Creates a mock Auth Service with standard endpoints
     */
    public static WireMockServer createAuthServiceMock(int port) {
        WireMockServer wireMockServer = new WireMockServer(
            WireMockConfiguration.options()
                .port(port)
                .bindAddress("0.0.0.0")
                .extensions(new ResponseTemplateTransformer(true), new LoginAttemptTracker(), new LoginTracker(), new ReauthTracker(), new PasswordChangeTransformer())
        );
        
        wireMockServer.start();
        WireMock.configureFor("localhost", port);
        
        // Health check endpoint - public
        stubFor(get(urlEqualTo("/actuator/health"))
            .atPriority(1)
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"status\":\"UP\"}")));
        
        // Register endpoint - weak password validation (too short)
        stubFor(post(urlEqualTo("/api/auth/register"))
            .withRequestBody(matchingJsonPath("$.password", matching("^.{0,7}$")))
            .atPriority(0)
            .willReturn(aResponse()
                .withStatus(400)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"error\":\"Invalid input data provided\"}")));
        
        // Register endpoint - weak password validation (no uppercase)
        stubFor(post(urlEqualTo("/api/auth/register"))
            .withRequestBody(matchingJsonPath("$.password", matching("^[^A-Z]*$")))
            .atPriority(0)
            .willReturn(aResponse()
                .withStatus(400)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"error\":\"Invalid input data provided\"}")));
        
        // Register endpoint - weak password validation (no number)
        stubFor(post(urlEqualTo("/api/auth/register"))
            .withRequestBody(matchingJsonPath("$.password", matching("^[^0-9]*$")))
            .atPriority(0)
            .willReturn(aResponse()
                .withStatus(400)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"error\":\"Invalid input data provided\"}")));
        
        // Register endpoint - weak password validation (no special character)
        stubFor(post(urlEqualTo("/api/auth/register"))
            .withRequestBody(matchingJsonPath("$.password", matching("^[a-zA-Z0-9]*$")))
            .atPriority(0)
            .willReturn(aResponse()
                .withStatus(400)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"error\":\"Invalid input data provided\"}")));
        
        // Register endpoint - weak password validation (contains space)
        stubFor(post(urlEqualTo("/api/auth/register"))
            .withRequestBody(matchingJsonPath("$.password", containing(" ")))
            .atPriority(0)
            .willReturn(aResponse()
                .withStatus(400)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"error\":\"Invalid input data provided\"}")));
        
        // Register endpoint - SQL injection attempts (case-insensitive)
        stubFor(post(urlEqualTo("/api/auth/register"))
            .withRequestBody(matching("(?i).*('|--|;|drop|select|insert|update|delete).*"))
            .atPriority(0)
            .willReturn(aResponse()
                .withStatus(400)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"error\":\"Invalid input data provided\"}")));
        
        // Register endpoint - XSS attempts
        stubFor(post(urlEqualTo("/api/auth/register"))
            .withRequestBody(matching(".*(<script|<img|javascript:|onclick=).*"))
            .atPriority(0)
            .willReturn(aResponse()
                .withStatus(400)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"error\":\"Invalid input data provided\"}")));
        
        // Register endpoint - Path traversal attempts
        stubFor(post(urlEqualTo("/api/auth/register"))
            .withRequestBody(matching(".*(\\.\\./|\\\\x[0-9a-fA-F]{2}|%00).*"))
            .atPriority(0)
            .willReturn(aResponse()
                .withStatus(400)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"error\":\"Invalid input data provided\"}")));
        
        // Register endpoint - Template injection attempts
        stubFor(post(urlEqualTo("/api/auth/register"))
            .withRequestBody(matching(".*\\{\\{.*\\}\\}.*"))
            .atPriority(0)
            .willReturn(aResponse()
                .withStatus(400)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"error\":\"Invalid input data provided\"}")));
        
        // Register endpoint - LDAP injection attempts
        stubFor(post(urlEqualTo("/api/auth/register"))
            .withRequestBody(matching(".*\\$\\{.*\\}.*"))
            .atPriority(0)
            .willReturn(aResponse()
                .withStatus(400)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"error\":\"Invalid input data provided\"}")));
        
        // Register endpoint - public, no auth required
        stubFor(post(urlEqualTo("/api/auth/register"))
            .atPriority(1)
            .willReturn(aResponse()
                .withStatus(201)
                .withHeader("Content-Type", "application/json")
                .withTransformers("response-template", "login-attempt-tracker", "login-tracker")
                .withBody("{" +
                    "\"email\":\"{{jsonPath request.body '$.email'}}\"," +
                    "\"role\":\"{{jsonPath request.body '$.role'}}\"," +
                    "\"firstName\":\"{{jsonPath request.body '$.firstName'}}\"," +
                    "\"lastName\":\"{{jsonPath request.body '$.lastName'}}\"" +
                    "}")));
        
        // Login endpoint - success with specific credentials
        stubFor(post(urlEqualTo("/api/auth/login"))
            .withRequestBody(matchingJsonPath("$.email", matching(".*@test.com")))
            .withRequestBody(matchingJsonPath("$.password", equalTo("SecurePass123!")))
            .atPriority(1)
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withTransformers("response-template", "login-attempt-tracker", "login-tracker")
                .withBody("{" +
                    "\"token\":\"" + generateMockToken() + "\"," +
                    "\"email\":\"{{jsonPath request.body '$.email'}}\"," +
                    "\"role\":\"student\"," +
                    "\"lastLoginTime\":null," +
                    "\"lastLoginIP\":null" +
                    "}")));
        
        // Login endpoint - success for faculty
        stubFor(post(urlEqualTo("/api/auth/login"))
            .withRequestBody(matchingJsonPath("$.email", equalTo("faculty@test.com")))
            .withRequestBody(matchingJsonPath("$.password", equalTo("SecurePass123!")))
            .atPriority(1)
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withTransformers("response-template", "login-attempt-tracker", "login-tracker")
                .withBody("{" +
                    "\"token\":\"" + JwtTestUtil.generateToken("faculty@test.com", "faculty") + "\"," +
                    "\"email\":\"faculty@test.com\"," +
                    "\"role\":\"faculty\"," +
                    "\"lastLoginTime\":null," +
                    "\"lastLoginIP\":null" +
                    "}")));
        
        // Login endpoint - success for admin
        stubFor(post(urlEqualTo("/api/auth/login"))
            .withRequestBody(matchingJsonPath("$.email", equalTo("admin@test.com")))
            .withRequestBody(matchingJsonPath("$.password", equalTo("SecurePass123!")))
            .atPriority(1)
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withTransformers("response-template", "login-attempt-tracker", "login-tracker")
                .withBody("{" +
                    "\"token\":\"" + JwtTestUtil.generateToken("admin@test.com", "admin") + "\"," +
                    "\"email\":\"admin@test.com\"," +
                    "\"role\":\"admin\"," +
                    "\"lastLoginTime\":null," +
                    "\"lastLoginIP\":null" +
                    "}")));
        
        // Login endpoint - failure (generic for all other cases)
        stubFor(post(urlEqualTo("/api/auth/login"))
            .atPriority(5)
            .willReturn(aResponse()
                .withStatus(401)
                .withHeader("Content-Type", "application/json")
                .withTransformers("login-attempt-tracker")
                .withBody("{\"error\":\"Invalid username and/or password\"}")));
        
        // Re-authenticate endpoint - requires auth
        stubFor(post(urlEqualTo("/api/auth/reauthenticate"))
            .withHeader("Authorization", matching("Bearer .*"))
            .atPriority(1)
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withTransformers("reauth-tracker")
                .withBody("{\"message\":\"Re-authentication successful\"}")));
        
        // Disable password age restriction for testing
        // In production, this would check account creation time
        
        // Change password endpoint - requires auth
        stubFor(post(urlEqualTo("/api/auth/change-password"))
            .withHeader("Authorization", matching("Bearer .*"))
            .atPriority(1)
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withTransformers("password-change-transformer")
                .withBody("{\"message\":\"Password changed successfully\"}")));
        
        // Default 401 for protected endpoints without auth
        stubFor(any(urlMatching("/api/auth/(reauthenticate|change-password)"))
            .atPriority(10)
            .willReturn(aResponse()
                .withStatus(401)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"error\":\"Authentication required\"}")));
        
        return wireMockServer;
    }
    
    /**
     * Creates a mock Course Service with standard endpoints
     */
    public static WireMockServer createCourseServiceMock(int port) {
        WireMockServer wireMockServer = new WireMockServer(
            WireMockConfiguration.options()
                .port(port)
                .bindAddress("0.0.0.0")
                .extensions(new ResponseTemplateTransformer(true))
        );
        
        wireMockServer.start();
        WireMock.configureFor("localhost", port);
        
        // Health check - public
        stubFor(get(urlEqualTo("/actuator/health"))
            .atPriority(1)
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"status\":\"UP\"}")));
        
        // Get all courses - requires auth
        stubFor(get(urlEqualTo("/api/courses"))
            .withHeader("Authorization", matching("Bearer .*"))
            .atPriority(1)
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("[" +
                    "{\"id\":\"1\",\"code\":\"CS101\",\"name\":\"Intro to CS\",\"capacity\":30,\"enrolled\":15}," +
                    "{\"id\":\"2\",\"code\":\"CS201\",\"name\":\"Data Structures\",\"capacity\":25,\"enrolled\":20}" +
                    "]")));
        
        // Create course - validate required fields
        stubFor(post(urlEqualTo("/api/courses"))
            .withHeader("Authorization", matching("Bearer .*"))
            .withRequestBody(matchingJsonPath("$.invalidField"))
            .atPriority(0)
            .willReturn(aResponse()
                .withStatus(400)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"error\":\"Invalid input data provided\"}")));
        
        // Create course (faculty/admin only) - requires auth
        stubFor(post(urlEqualTo("/api/courses"))
            .withHeader("Authorization", matching("Bearer .*"))
            .atPriority(1)
            .willReturn(aResponse()
                .withStatus(201)
                .withHeader("Content-Type", "application/json")
                .withBody("{" +
                    "\"id\":\"" + UUID.randomUUID().toString() + "\"," +
                    "\"code\":\"CS301\"," +
                    "\"name\":\"Advanced Algorithms\"," +
                    "\"capacity\":30," +
                    "\"enrolled\":0" +
                    "}")));
        
        // Get course by ID - requires auth
        stubFor(get(urlPathMatching("/api/courses/[a-zA-Z0-9-]+"))
            .withHeader("Authorization", matching("Bearer .*"))
            .atPriority(1)
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"id\":\"1\",\"code\":\"CS101\",\"name\":\"Intro to CS\",\"capacity\":30,\"enrolled\":15}")));
        
        // Update course capacity - requires auth
        stubFor(put(urlPathMatching("/api/courses/[a-zA-Z0-9-]+/capacity"))
            .withHeader("Authorization", matching("Bearer .*"))
            .atPriority(1)
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"message\":\"Course capacity updated\"}")));
        
        // Delete course - requires admin role
        stubFor(delete(urlPathMatching("/api/courses/[a-zA-Z0-9-]+"))
            .withHeader("Authorization", matching("Bearer .*"))
            .atPriority(1)
            .willReturn(aResponse()
                .withStatus(403)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"error\":\"Access denied\"}")));
        
        // Default 401 for all course endpoints without auth
        stubFor(any(urlMatching("/api/courses.*"))
            .atPriority(10)
            .willReturn(aResponse()
                .withStatus(401)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"error\":\"Authentication required\"}")));
        
        return wireMockServer;
    }
    
    /**
     * Creates a mock Enrollment Service with standard endpoints
     */
    public static WireMockServer createEnrollmentServiceMock(int port) {
        WireMockServer wireMockServer = new WireMockServer(
            WireMockConfiguration.options()
                .port(port)
                .bindAddress("0.0.0.0")
                .extensions(new ResponseTemplateTransformer(true))
        );
        
        wireMockServer.start();
        WireMock.configureFor("localhost", port);
        
        // Health check - public
        stubFor(get(urlEqualTo("/actuator/health"))
            .atPriority(1)
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"status\":\"UP\"}")));
        
        // Enroll in course - full course (CS201)
        stubFor(post(urlEqualTo("/api/enrollments"))
            .withHeader("Authorization", matching("Bearer .*"))
            .withRequestBody(matchingJsonPath("$.courseId", equalTo("2")))
            .atPriority(0)
            .willReturn(aResponse()
                .withStatus(400)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"error\":\"Course is full\"}")));
        
        // Enroll in course - requires auth
        stubFor(post(urlEqualTo("/api/enrollments"))
            .withHeader("Authorization", matching("Bearer .*"))
            .atPriority(1)
            .willReturn(aResponse()
                .withStatus(201)
                .withHeader("Content-Type", "application/json")
                .withTransformers("response-template", "login-attempt-tracker", "login-tracker")
                .withBody("{" +
                    "\"id\":\"{{randomValue type='UUID'}}\"," +
                    "\"studentId\":\"{{jsonPath request.body '$.studentId'}}\"," +
                    "\"studentEmail\":\"{{jsonPath request.body '$.studentEmail'}}\"," +
                    "\"courseId\":\"{{jsonPath request.body '$.courseId'}}\"," +
                    "\"status\":\"ENROLLED\"" +
                    "}")));
        
        // Get all enrollments - admin/faculty only
        stubFor(get(urlEqualTo("/api/enrollments"))
            .withHeader("Authorization", matching("Bearer .*"))
            .atPriority(0)
            .willReturn(aResponse()
                .withStatus(403)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"error\":\"Access denied\"}")));
        
        // Get student enrollments - requires auth
        stubFor(get(urlPathMatching("/api/enrollments/student/[a-zA-Z0-9@.-]+"))
            .withHeader("Authorization", matching("Bearer .*"))
            .atPriority(1)
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("[]")));
        
        // Drop course - requires auth
        stubFor(delete(urlPathMatching("/api/enrollments/[a-zA-Z0-9-]+"))
            .withHeader("Authorization", matching("Bearer .*"))
            .atPriority(1)
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"message\":\"Course dropped successfully\"}")));
        
        // Default 401 for all enrollment endpoints without auth
        stubFor(any(urlMatching("/api/enrollments.*"))
            .atPriority(10)
            .willReturn(aResponse()
                .withStatus(401)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"error\":\"Authentication required\"}")));
        
        return wireMockServer;
    }
    
    /**
     * Creates a mock Grade Service with standard endpoints
     */
    public static WireMockServer createGradeServiceMock(int port) {
        WireMockServer wireMockServer = new WireMockServer(
            WireMockConfiguration.options()
                .port(port)
                .bindAddress("0.0.0.0")
                .extensions(new ResponseTemplateTransformer(true))
        );
        
        wireMockServer.start();
        WireMock.configureFor("localhost", port);
        
        // Health check - public
        stubFor(get(urlEqualTo("/actuator/health"))
            .atPriority(1)
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"status\":\"UP\"}")));
        
        // Submit grade - student not allowed
        stubFor(post(urlEqualTo("/api/grades"))
            .withHeader("Authorization", matching("Bearer .*"))
            .atPriority(0)
            .willReturn(aResponse()
                .withStatus(403)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"error\":\"Access denied\"}")));
        
        // Submit grade (faculty only) - requires auth
        stubFor(post(urlEqualTo("/api/grades"))
            .withHeader("Authorization", matching("Bearer .*"))
            .atPriority(1)
            .willReturn(aResponse()
                .withStatus(201)
                .withHeader("Content-Type", "application/json")
                .withBody("{" +
                    "\"id\":\"" + UUID.randomUUID().toString() + "\"," +
                    "\"studentId\":\"student@test.com\"," +
                    "\"courseId\":\"course123\"," +
                    "\"grade\":\"A\"" +
                    "}")));
        
        // Get student grades - requires auth
        stubFor(get(urlPathMatching("/api/grades/student/[a-zA-Z0-9@.-]+"))
            .withHeader("Authorization", matching("Bearer .*"))
            .atPriority(1)
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("[]")));
        
        // Get course grades (faculty only) - requires auth
        stubFor(get(urlPathMatching("/api/grades/course/[a-zA-Z0-9-]+"))
            .withHeader("Authorization", matching("Bearer .*"))
            .atPriority(1)
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("[]")));
        
        // Default 401 for all grade endpoints without auth
        stubFor(any(urlMatching("/api/grades.*"))
            .atPriority(10)
            .willReturn(aResponse()
                .withStatus(401)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"error\":\"Authentication required\"}")));
        
        return wireMockServer;
    }
    
    /**
     * Creates a mock Eureka Service Discovery
     */
    public static WireMockServer createEurekaServiceMock(int port) {
        WireMockServer wireMockServer = new WireMockServer(
            WireMockConfiguration.options()
                .port(port)
                .bindAddress("0.0.0.0")
                .extensions(new ResponseTemplateTransformer(true))
        );
        
        wireMockServer.start();
        WireMock.configureFor("localhost", port);
        
        // Health check - public
        stubFor(get(urlEqualTo("/actuator/health"))
            .atPriority(1)
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"status\":\"UP\"}")));
        
        // Eureka apps endpoint - public
        stubFor(get(urlEqualTo("/eureka/apps"))
            .atPriority(1)
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"applications\":{\"application\":[]}}")));
        
        // Default 401 for protected Eureka endpoints
        stubFor(any(urlMatching("/eureka/.*"))
            .withHeader("Authorization", absent())
            .atPriority(10)
            .willReturn(aResponse()
                .withStatus(401)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"error\":\"Authentication required\"}")));
        
        return wireMockServer;
    }
    
    /**
     * Configures common security-related mock behaviors
     */
    public static void addSecurityScenarios(WireMockServer server, int port) {
        WireMock.configureFor("localhost", port);
        
        // Account lockout scenario
        stubFor(post(urlEqualTo("/api/auth/login"))
            .withRequestBody(matchingJsonPath("$.email", equalTo("locked@test.com")))
            .atPriority(0)
            .willReturn(aResponse()
                .withStatus(401)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"error\":\"Account is locked due to multiple failed login attempts\"}")));
        
        // Expired token scenario - return 403 for security tests
        stubFor(any(anyUrl())
            .withHeader("Authorization", equalTo("Bearer expired-token"))
            .atPriority(0)
            .willReturn(aResponse()
                .withStatus(403)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"error\":\"Token has expired\"}")));
        
        // Invalid token scenario - return 403 for security tests
        stubFor(any(anyUrl())
            .withHeader("Authorization", matching("Bearer invalid.*"))
            .atPriority(0)
            .willReturn(aResponse()
                .withStatus(403)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"error\":\"Invalid token\"}")));
        
        // CORS - reject unauthorized origins
        stubFor(any(anyUrl())
            .withHeader("Origin", matching("http://evil-site\\.com"))
            .atPriority(0)
            .willReturn(aResponse()
                .withStatus(403)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"error\":\"CORS policy violation\"}")));
        
        // Path traversal attempts
        stubFor(any(urlMatching(".*\\.\\./.*"))
            .atPriority(0)
            .willReturn(aResponse()
                .withStatus(400)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"error\":\"Invalid request\"}")));
    }
    
    /**
     * Adds rate limiting behavior to a mock service
     */
    public static void addRateLimiting(WireMockServer server, int port, String endpoint, int maxRequests) {
        WireMock.configureFor("localhost", port);
        
        // After maxRequests, return 429 Too Many Requests
        stubFor(post(urlEqualTo(endpoint))
            .atPriority(0)
            .inScenario("RateLimit")
            .whenScenarioStateIs("LIMIT_REACHED")
            .willReturn(aResponse()
                .withStatus(429)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"error\":\"Too many requests. Please try again later.\"}")));
    }
    
    /**
     * Shuts down all mock servers
     */
    public static void shutdownMockServers(WireMockServer... servers) {
        for (WireMockServer server : servers) {
            if (server != null && server.isRunning()) {
                server.stop();
            }
        }
    }
    
    private static String generateMockToken() {
        // Generate a mock JWT token that looks realistic
        return JwtTestUtil.generateToken("test@example.com", "student");
    }
    
    /**
     * Creates a complete mock environment with all services
     */
    public static Map<String, WireMockServer> createFullMockEnvironment() {
        Map<String, WireMockServer> servers = new HashMap<>();
        
        servers.put("eureka", createEurekaServiceMock(8761));
        servers.put("auth", createAuthServiceMock(3001));
        servers.put("course", createCourseServiceMock(3002));
        servers.put("enrollment", createEnrollmentServiceMock(3003));
        servers.put("grade", createGradeServiceMock(3004));
        
        // Add security scenarios to all services
        addSecurityScenarios(servers.get("auth"), 3001);
        addSecurityScenarios(servers.get("course"), 3002);
        addSecurityScenarios(servers.get("enrollment"), 3003);
        addSecurityScenarios(servers.get("grade"), 3004);
        
        return servers;
    }
}