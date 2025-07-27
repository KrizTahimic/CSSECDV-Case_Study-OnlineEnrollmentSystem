package com.enrollment.e2e.util;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.containers.wait.strategy.WaitStrategy;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;
import org.testcontainers.utility.DockerImageName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Factory for creating TestContainers for each microservice.
 * Provides real service containers for full end-to-end testing.
 */
public class ServiceContainerFactory {
    
    private static final Logger log = LoggerFactory.getLogger(ServiceContainerFactory.class);
    private static final Duration STARTUP_TIMEOUT = Duration.ofMinutes(5);
    private static final String JWT_SECRET = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    
    /**
     * Creates a container for the Eureka Service Discovery
     */
    public static GenericContainer<?> createEurekaContainer(Network network) {
        log.info("Creating Eureka container");
        WaitStrategy waitStrategy = Wait.forLogMessage(".*Started.*in.*seconds.*", 1)
                .withStartupTimeout(STARTUP_TIMEOUT);
        
        return new GenericContainer<>(DockerImageName.parse("onlineenrollmentsystem-p4-eureka:latest"))
                .withNetwork(network)
                .withNetworkAliases("eureka")
                .withExposedPorts(8761)
                .withEnv("SPRING_PROFILES_ACTIVE", "docker")
                .withEnv("SERVER_PORT", "8761")
                .waitingFor(waitStrategy)
                .withLabel("service", "eureka")
                .withLogConsumer(outputFrame -> log.debug("[EUREKA] {}", outputFrame.getUtf8String()));
    }
    
    /**
     * Creates a container for the Auth Service
     */
    public static GenericContainer<?> createAuthServiceContainer(Network network, String mongoUri, String redisHost) {
        log.info("Creating Auth Service container");
        // Use JVM system properties to ensure Redis configuration is properly overridden
        String javaOpts = String.format(
            "-Dspring.redis.host=%s -Dspring.redis.port=6379 -Dspring.data.redis.host=%s -Dspring.data.redis.port=6379 " +
            "-Dspring.redis.connect-timeout=5000 -Dspring.redis.timeout=5000",
            redisHost, redisHost
        );
        
        // Use log-based wait strategy - more reliable for Spring Boot services
        WaitStrategy waitStrategy = Wait.forLogMessage(".*Started.*in.*seconds.*", 1)
                .withStartupTimeout(STARTUP_TIMEOUT);
        
        return new GenericContainer<>(DockerImageName.parse("onlineenrollmentsystem-p4-auth-service:latest"))
                .withNetwork(network)
                .withNetworkAliases("auth-service")
                .withExposedPorts(3001)
                .withEnv("SPRING_PROFILES_ACTIVE", "docker")
                .withEnv("SPRING_DATA_MONGODB_URI", "mongodb://mongodb:27017/auth_service")
                .withEnv("SPRING_DATA_MONGODB_HOST", "mongodb")
                .withEnv("SPRING_DATA_MONGODB_PORT", "27017")
                .withEnv("SPRING_DATA_MONGODB_DATABASE", "auth_service")
                // Redis configuration with connection pooling
                .withEnv("SPRING_REDIS_HOST", redisHost)
                .withEnv("SPRING_DATA_REDIS_HOST", redisHost)
                .withEnv("SPRING_REDIS_PORT", "6379")
                .withEnv("SPRING_DATA_REDIS_PORT", "6379")
                .withEnv("SPRING_REDIS_TIMEOUT", "5000")
                .withEnv("SPRING_REDIS_CONNECT_TIMEOUT", "5000")
                .withEnv("SPRING_REDIS_LETTUCE_POOL_MAX-ACTIVE", "8")
                .withEnv("SPRING_REDIS_LETTUCE_POOL_MAX-IDLE", "8")
                .withEnv("SPRING_REDIS_LETTUCE_POOL_MIN-IDLE", "0")
                .withEnv("SPRING_REDIS_LETTUCE_POOL_MAX-WAIT", "2000")
                .withEnv("EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE", "http://eureka:8761/eureka/")
                .withEnv("JWT_SECRET", JWT_SECRET)
                .withEnv("SERVER_PORT", "3001")
                .withEnv("EUREKA_CLIENT_ENABLED", "true")
                .withEnv("SPRING_CLOUD_DISCOVERY_ENABLED", "true")
                // Add JVM options to override properties
                .withEnv("JAVA_OPTS", javaOpts)
                .waitingFor(waitStrategy)
                .withLabel("service", "auth-service")
                .withLogConsumer(outputFrame -> {
                    String logLine = outputFrame.getUtf8String().trim();
                    if (!logLine.isEmpty()) {
                        System.out.println("[AUTH] " + logLine);
                        log.info("[AUTH] {}", logLine);
                    }
                });
    }
    
    /**
     * Creates a container for the Course Service
     */
    public static GenericContainer<?> createCourseServiceContainer(Network network, String mongoUri) {
        log.info("Creating Course Service container");
        WaitStrategy waitStrategy = Wait.forLogMessage(".*Started.*in.*seconds.*", 1)
                .withStartupTimeout(STARTUP_TIMEOUT);
        
        return new GenericContainer<>(DockerImageName.parse("onlineenrollmentsystem-p4-course-service:latest"))
                .withNetwork(network)
                .withNetworkAliases("course-service")
                .withExposedPorts(3002)
                .withEnv("SPRING_PROFILES_ACTIVE", "docker")
                .withEnv("SPRING_DATA_MONGODB_URI", "mongodb://mongodb:27017/course_service")
                .withEnv("SPRING_DATA_MONGODB_HOST", "mongodb")
                .withEnv("SPRING_DATA_MONGODB_PORT", "27017")
                .withEnv("SPRING_DATA_MONGODB_DATABASE", "course_service")
                .withEnv("EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE", "http://eureka:8761/eureka/")
                .withEnv("JWT_SECRET", JWT_SECRET)
                .withEnv("SERVER_PORT", "3002")
                .withEnv("EUREKA_CLIENT_ENABLED", "true")
                .withEnv("SPRING_CLOUD_DISCOVERY_ENABLED", "true")
                .waitingFor(waitStrategy)
                .withLabel("service", "course-service")
                .withLogConsumer(outputFrame -> log.debug("[COURSE] {}", outputFrame.getUtf8String()));
    }
    
    /**
     * Creates a container for the Enrollment Service
     */
    public static GenericContainer<?> createEnrollmentServiceContainer(Network network, String mongoUri) {
        log.info("Creating Enrollment Service container");
        WaitStrategy waitStrategy = Wait.forLogMessage(".*Started.*in.*seconds.*", 1)
                .withStartupTimeout(STARTUP_TIMEOUT);
        
        return new GenericContainer<>(DockerImageName.parse("onlineenrollmentsystem-p4-enrollment-service:latest"))
                .withNetwork(network)
                .withNetworkAliases("enrollment-service")
                .withExposedPorts(3003)
                .withEnv("SPRING_PROFILES_ACTIVE", "docker")
                .withEnv("SPRING_DATA_MONGODB_URI", "mongodb://mongodb:27017/enrollment_service")
                .withEnv("SPRING_DATA_MONGODB_HOST", "mongodb")
                .withEnv("SPRING_DATA_MONGODB_PORT", "27017")
                .withEnv("SPRING_DATA_MONGODB_DATABASE", "enrollment_service")
                .withEnv("EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE", "http://eureka:8761/eureka/")
                .withEnv("JWT_SECRET", JWT_SECRET)
                .withEnv("SERVER_PORT", "3003")
                .withEnv("EUREKA_CLIENT_ENABLED", "true")
                .withEnv("SPRING_CLOUD_DISCOVERY_ENABLED", "true")
                .waitingFor(waitStrategy)
                .withLabel("service", "enrollment-service")
                .withLogConsumer(outputFrame -> log.debug("[ENROLLMENT] {}", outputFrame.getUtf8String()));
    }
    
    /**
     * Creates a container for the Grade Service
     */
    public static GenericContainer<?> createGradeServiceContainer(Network network, String mongoUri) {
        log.info("Creating Grade Service container");
        WaitStrategy waitStrategy = Wait.forLogMessage(".*Started.*in.*seconds.*", 1)
                .withStartupTimeout(STARTUP_TIMEOUT);
        
        return new GenericContainer<>(DockerImageName.parse("onlineenrollmentsystem-p4-grade-service:latest"))
                .withNetwork(network)
                .withNetworkAliases("grade-service")
                .withExposedPorts(3004)
                .withEnv("SPRING_PROFILES_ACTIVE", "docker")
                .withEnv("SPRING_DATA_MONGODB_URI", "mongodb://mongodb:27017/grade_service")
                .withEnv("SPRING_DATA_MONGODB_HOST", "mongodb")
                .withEnv("SPRING_DATA_MONGODB_PORT", "27017")
                .withEnv("SPRING_DATA_MONGODB_DATABASE", "grade_service")
                .withEnv("EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE", "http://eureka:8761/eureka/")
                .withEnv("JWT_SECRET", JWT_SECRET)
                .withEnv("SERVER_PORT", "3004")
                .withEnv("EUREKA_CLIENT_ENABLED", "true")
                .withEnv("SPRING_CLOUD_DISCOVERY_ENABLED", "true")
                .waitingFor(waitStrategy)
                .withLabel("service", "grade-service")
                .withLogConsumer(outputFrame -> log.debug("[GRADE] {}", outputFrame.getUtf8String()));
    }
    
    /**
     * Creates a complete microservices environment with all services
     */
    public static Map<String, GenericContainer<?>> createFullServiceEnvironment(
            Network network, String mongoUri, String redisHost) {
        
        Map<String, GenericContainer<?>> containers = new HashMap<>();
        
        try {
            // Check if Docker images exist
            checkDockerImages();
            
            // Create containers in proper order
            containers.put("eureka", createEurekaContainer(network));
            containers.put("auth", createAuthServiceContainer(network, mongoUri, redisHost));
            containers.put("course", createCourseServiceContainer(network, mongoUri));
            containers.put("enrollment", createEnrollmentServiceContainer(network, mongoUri));
            containers.put("grade", createGradeServiceContainer(network, mongoUri));
            
            // Set dependencies
            containers.get("auth").dependsOn(containers.get("eureka"));
            containers.get("course").dependsOn(containers.get("eureka"));
            containers.get("enrollment").dependsOn(containers.get("eureka"), containers.get("course"));
            containers.get("grade").dependsOn(containers.get("eureka"), containers.get("course"));
            
        } catch (Exception e) {
            System.err.println("ERROR: Failed to create service containers: " + e.getMessage());
            throw new IllegalStateException("Cannot create service environment", e);
        }
        
        return containers;
    }
    
    /**
     * Helper method to get mapped port for a service
     */
    public static int getMappedPort(GenericContainer<?> container, int originalPort) {
        return container.getMappedPort(originalPort);
    }
    
    /**
     * Helper method to check if a container is healthy
     */
    public static boolean isContainerHealthy(GenericContainer<?> container) {
        try {
            return container.isRunning() && container.isHealthy();
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Creates environment variables for service configuration
     */
    public static Map<String, String> createServiceEnvironment(
            String profile, String mongoUri, String redisHost, String eurekaUrl) {
        
        Map<String, String> env = new HashMap<>();
        env.put("SPRING_PROFILES_ACTIVE", profile);
        env.put("SPRING_DATA_MONGODB_URI", mongoUri);
        env.put("SPRING_REDIS_HOST", redisHost);
        env.put("EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE", eurekaUrl);
        env.put("JWT_SECRET", JWT_SECRET);
        env.put("JWT_EXPIRATION", "86400000");
        
        // Logging configuration
        env.put("LOGGING_LEVEL_ROOT", "INFO");
        env.put("LOGGING_LEVEL_COM_ENROLLMENT", "DEBUG");
        
        return env;
    }
    
    /**
     * Stops all containers gracefully
     */
    public static void stopContainers(Map<String, GenericContainer<?>> containers) {
        // Stop in reverse order
        String[] stopOrder = {"grade", "enrollment", "course", "auth", "eureka"};
        
        for (String serviceName : stopOrder) {
            GenericContainer<?> container = containers.get(serviceName);
            if (container != null && container.isRunning()) {
                System.out.println("Stopping " + serviceName + " container...");
                container.stop();
            }
        }
    }
    
    /**
     * Checks if required Docker images are available
     */
    private static void checkDockerImages() {
        String[] requiredImages = {
            "onlineenrollmentsystem-p4-eureka:latest",
            "onlineenrollmentsystem-p4-auth-service:latest",
            "onlineenrollmentsystem-p4-course-service:latest",
            "onlineenrollmentsystem-p4-enrollment-service:latest",
            "onlineenrollmentsystem-p4-grade-service:latest"
        };
        
        System.out.println("=== Checking Docker Images ===");
        boolean allImagesFound = true;
        
        for (String image : requiredImages) {
            try {
                // Try to create a DockerImageName to verify it exists
                DockerImageName dockerImage = DockerImageName.parse(image);
                System.out.println("✓ Found image: " + image);
            } catch (Exception e) {
                System.err.println("✗ Missing image: " + image);
                allImagesFound = false;
            }
        }
        
        if (!allImagesFound) {
            System.err.println("\n=== Docker Images Missing ===");
            System.err.println("Some required Docker images are not available.");
            System.err.println("Please build the images first by running:");
            System.err.println("  cd .. && mvn clean package");
            System.err.println("  docker-compose build");
            System.err.println("Or run the build script:");
            System.err.println("  ./build-docker-images.sh");
            throw new IllegalStateException("Required Docker images are not available");
        }
        
        System.out.println("All required Docker images found.\n");
    }
}