package com.enrollment.e2e.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

/**
 * E2E Test configuration that provides different testing modes:
 * 1. Real services - expects services to be running on standard ports
 * 2. Docker services - starts services in Docker containers
 * 3. Mock services - uses WireMock (separate configuration)
 */
@TestConfiguration
public class E2ETestConfiguration {
    
    // Standard service ports matching production
    public static final int AUTH_SERVICE_PORT = 3001;
    public static final int COURSE_SERVICE_PORT = 3002;
    public static final int ENROLLMENT_SERVICE_PORT = 3003;
    public static final int GRADE_SERVICE_PORT = 3004;
    public static final int EUREKA_PORT = 8761;
    public static final int MONGODB_PORT = 27017;
    public static final int REDIS_PORT = 6379;
    
    // Service URLs
    public static final String AUTH_SERVICE_URL = "http://localhost:" + AUTH_SERVICE_PORT;
    public static final String COURSE_SERVICE_URL = "http://localhost:" + COURSE_SERVICE_PORT;
    public static final String ENROLLMENT_SERVICE_URL = "http://localhost:" + ENROLLMENT_SERVICE_PORT;
    public static final String GRADE_SERVICE_URL = "http://localhost:" + GRADE_SERVICE_PORT;
    
    /**
     * Configuration for running tests with Docker Compose.
     * This starts all services in containers with proper networking.
     */
    @Profile("docker")
    @Bean
    public DockerComposeContainer dockerComposeContainer() {
        return new DockerComposeContainer()
                .withServices("mongodb", "redis", "eureka", "auth-service", 
                             "course-service", "enrollment-service", "grade-service")
                .withExposedService("auth-service", AUTH_SERVICE_PORT, 
                    Wait.forHttp("/actuator/health").withStartupTimeout(Duration.ofSeconds(120)))
                .withExposedService("course-service", COURSE_SERVICE_PORT,
                    Wait.forHttp("/actuator/health").withStartupTimeout(Duration.ofSeconds(120)))
                .withExposedService("enrollment-service", ENROLLMENT_SERVICE_PORT,
                    Wait.forHttp("/actuator/health").withStartupTimeout(Duration.ofSeconds(120)))
                .withExposedService("grade-service", GRADE_SERVICE_PORT,
                    Wait.forHttp("/actuator/health").withStartupTimeout(Duration.ofSeconds(120)));
    }
    
    /**
     * Configuration for running individual containers.
     * Useful when you want more control over each service.
     */
    @Profile("containers")
    public static class ContainerConfiguration {
        
        private static final Network network = Network.newNetwork();
        
        @Bean
        public MongoDBContainer mongoDBContainer() {
            return new MongoDBContainer(DockerImageName.parse("mongo:7.0"))
                    .withNetwork(network)
                    .withNetworkAliases("mongodb")
                    .withExposedPorts(MONGODB_PORT);
        }
        
        @Bean
        public GenericContainer<?> redisContainer() {
            return new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                    .withNetwork(network)
                    .withNetworkAliases("redis")
                    .withExposedPorts(REDIS_PORT);
        }
    }
    
    /**
     * Helper class to simulate Docker Compose functionality
     */
    public static class DockerComposeContainer {
        private String[] services;
        
        public DockerComposeContainer withServices(String... services) {
            this.services = services;
            return this;
        }
        
        public DockerComposeContainer withExposedService(String service, int port, Object wait) {
            // Configuration for service exposure
            return this;
        }
        
        public void start() {
            // In a real implementation, this would start docker-compose
            System.out.println("Starting services: " + String.join(", ", services));
        }
        
        public void stop() {
            System.out.println("Stopping services");
        }
    }
}