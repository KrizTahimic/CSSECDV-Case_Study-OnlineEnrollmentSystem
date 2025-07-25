package com.enrollment.e2e.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

/**
 * Configuration that starts all microservices in containers for e2e testing.
 * This allows tests to run without manually starting services.
 */
@TestConfiguration
@Testcontainers
public class ServiceContainersConfig {

    private static final Network network = Network.newNetwork();
    
    // MongoDB Container
    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer(DockerImageName.parse("mongo:7.0"))
            .withNetwork(network)
            .withNetworkAliases("mongodb")
            .withExposedPorts(27017);

    // Redis Container
    @Container
    static GenericContainer<?> redisContainer = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withNetwork(network)
            .withNetworkAliases("redis")
            .withExposedPorts(6379);

    // Eureka Service Discovery Container
    @Container
    static GenericContainer<?> eurekaContainer = new GenericContainer<>(DockerImageName.parse("openjdk:17-jdk-slim"))
            .withNetwork(network)
            .withNetworkAliases("eureka")
            .withExposedPorts(8761)
            .withEnv("SPRING_PROFILES_ACTIVE", "docker")
            .withCommand("java", "-jar", "/app/service-discovery.jar")
            .waitingFor(Wait.forHttp("/").forPort(8761).withStartupTimeout(Duration.ofSeconds(60)));

    // Auth Service Container
    @Container
    static GenericContainer<?> authServiceContainer = new GenericContainer<>(DockerImageName.parse("openjdk:17-jdk-slim"))
            .withNetwork(network)
            .withNetworkAliases("auth-service")
            .withExposedPorts(3001)
            .withEnv("SPRING_DATA_MONGODB_URI", "mongodb://mongodb:27017/auth_service")
            .withEnv("SPRING_REDIS_HOST", "redis")
            .withEnv("EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE", "http://eureka:8761/eureka/")
            .withCommand("java", "-jar", "/app/auth-service.jar")
            .waitingFor(Wait.forHttp("/actuator/health").forPort(3001).withStartupTimeout(Duration.ofSeconds(90)))
            .dependsOn(mongoDBContainer, redisContainer, eurekaContainer);

    // Course Service Container
    @Container
    static GenericContainer<?> courseServiceContainer = new GenericContainer<>(DockerImageName.parse("openjdk:17-jdk-slim"))
            .withNetwork(network)
            .withNetworkAliases("course-service")
            .withExposedPorts(3002)
            .withEnv("SPRING_DATA_MONGODB_URI", "mongodb://mongodb:27017/course_service")
            .withEnv("EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE", "http://eureka:8761/eureka/")
            .withCommand("java", "-jar", "/app/course-service.jar")
            .waitingFor(Wait.forHttp("/actuator/health").forPort(3002).withStartupTimeout(Duration.ofSeconds(90)))
            .dependsOn(mongoDBContainer, eurekaContainer);

    // Enrollment Service Container
    @Container
    static GenericContainer<?> enrollmentServiceContainer = new GenericContainer<>(DockerImageName.parse("openjdk:17-jdk-slim"))
            .withNetwork(network)
            .withNetworkAliases("enrollment-service")
            .withExposedPorts(3003)
            .withEnv("SPRING_DATA_MONGODB_URI", "mongodb://mongodb:27017/enrollment_service")
            .withEnv("EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE", "http://eureka:8761/eureka/")
            .withCommand("java", "-jar", "/app/enrollment-service.jar")
            .waitingFor(Wait.forHttp("/actuator/health").forPort(3003).withStartupTimeout(Duration.ofSeconds(90)))
            .dependsOn(mongoDBContainer, eurekaContainer);

    // Grade Service Container
    @Container
    static GenericContainer<?> gradeServiceContainer = new GenericContainer<>(DockerImageName.parse("openjdk:17-jdk-slim"))
            .withNetwork(network)
            .withNetworkAliases("grade-service")
            .withExposedPorts(3004)
            .withEnv("SPRING_DATA_MONGODB_URI", "mongodb://mongodb:27017/grade_service")
            .withEnv("EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE", "http://eureka:8761/eureka/")
            .withCommand("java", "-jar", "/app/grade-service.jar")
            .waitingFor(Wait.forHttp("/actuator/health").forPort(3004).withStartupTimeout(Duration.ofSeconds(90)))
            .dependsOn(mongoDBContainer, eurekaContainer);

    static {
        // Start all containers
        mongoDBContainer.start();
        redisContainer.start();
        
        // Note: In a real scenario, you would need to:
        // 1. Build the service JARs first
        // 2. Copy them into the containers
        // 3. Or use pre-built Docker images of your services
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        // Set properties for test application context
        registry.add("test.services.auth.url", () -> 
            "http://localhost:" + authServiceContainer.getMappedPort(3001));
        registry.add("test.services.course.url", () -> 
            "http://localhost:" + courseServiceContainer.getMappedPort(3002));
        registry.add("test.services.enrollment.url", () -> 
            "http://localhost:" + enrollmentServiceContainer.getMappedPort(3003));
        registry.add("test.services.grade.url", () -> 
            "http://localhost:" + gradeServiceContainer.getMappedPort(3004));
    }
}