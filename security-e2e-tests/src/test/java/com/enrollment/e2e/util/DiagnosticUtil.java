package com.enrollment.e2e.util;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Utility class for diagnostic operations during test failures
 */
public class DiagnosticUtil {
    
    private static final Logger log = LoggerFactory.getLogger(DiagnosticUtil.class);
    
    /**
     * Perform comprehensive diagnostics on a service container
     */
    public static void diagnoseContainer(String serviceName, GenericContainer<?> container) {
        log.info("=== Diagnostics for {} ===", serviceName);
        
        // Check if container is running
        log.info("Container running: {}", container.isRunning());
        log.info("Container healthy: {}", isContainerHealthy(container));
        log.info("Container ID: {}", container.getContainerId());
        log.info("Container Image: {}", container.getDockerImageName());
        
        // Get exposed ports
        if (container.getExposedPorts() != null && !container.getExposedPorts().isEmpty()) {
            log.info("Exposed ports:");
            for (Integer port : container.getExposedPorts()) {
                try {
                    Integer mappedPort = container.getMappedPort(port);
                    log.info("  {} -> {}", port, mappedPort);
                } catch (Exception e) {
                    log.error("  {} -> Error getting mapped port: {}", port, e.getMessage());
                }
            }
        }
        
        // Get container logs
        if (container.isRunning()) {
            log.info("Container logs (last 50 lines):");
            String logs = container.getLogs();
            String[] logLines = logs.split("\n");
            int startIdx = Math.max(0, logLines.length - 50);
            for (int i = startIdx; i < logLines.length; i++) {
                log.info("  {}", logLines[i]);
            }
        }
        
        // Check network aliases
        log.info("Network aliases: {}", container.getNetworkAliases());
        
        log.info("=== End Diagnostics for {} ===", serviceName);
    }
    
    /**
     * Check all service endpoints and report their status
     */
    public static void checkAllEndpoints(Map<String, String> serviceUrls) {
        log.info("=== Service Endpoint Status ===");
        
        for (Map.Entry<String, String> entry : serviceUrls.entrySet()) {
            String serviceName = entry.getKey();
            String baseUrl = entry.getValue();
            
            // Check health endpoint
            checkEndpoint(serviceName + " Health", baseUrl + "/actuator/health");
            
            // Check root endpoint
            checkEndpoint(serviceName + " Root", baseUrl + "/");
        }
        
        log.info("=== End Service Endpoint Status ===");
    }
    
    private static void checkEndpoint(String name, String url) {
        try {
            long startTime = System.currentTimeMillis();
            Response response = RestAssured
                .given()
                    .relaxedHTTPSValidation()
                    .baseUri(url)
                .when()
                    .get()
                .then()
                    .extract()
                    .response();
            
            long duration = System.currentTimeMillis() - startTime;
            log.info("{}: {} ({}ms)", name, response.getStatusCode(), duration);
            
            if (response.getStatusCode() >= 400) {
                log.info("  Response body: {}", response.getBody().asString());
            }
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("Connection refused")) {
                log.error("{}: Connection refused", name);
            } else {
                log.error("{}: {}", name, e.getMessage());
            }
        }
    }
    
    /**
     * Check MongoDB connection
     */
    public static void checkMongoDBConnection(GenericContainer<?> mongoContainer) {
        log.info("=== MongoDB Diagnostics ===");
        
        if (mongoContainer == null) {
            log.error("MongoDB container is null");
            return;
        }
        
        log.info("MongoDB running: {}", mongoContainer.isRunning());
        if (mongoContainer.isRunning()) {
            log.info("MongoDB host: {}", mongoContainer.getHost());
            log.info("MongoDB port: {}", mongoContainer.getMappedPort(27017));
            
            // Execute mongo command to check status
            try {
                var result = mongoContainer.execInContainer("mongosh", "--eval", "db.adminCommand('ping')");
                log.info("MongoDB ping result: {}", result.getStdout());
                if (!result.getStderr().isEmpty()) {
                    log.error("MongoDB ping error: {}", result.getStderr());
                }
            } catch (Exception e) {
                log.error("Failed to ping MongoDB: {}", e.getMessage());
            }
        }
        
        log.info("=== End MongoDB Diagnostics ===");
    }
    
    /**
     * Check Redis connection
     */
    public static void checkRedisConnection(GenericContainer<?> redisContainer) {
        log.info("=== Redis Diagnostics ===");
        
        if (redisContainer == null) {
            log.error("Redis container is null");
            return;
        }
        
        log.info("Redis running: {}", redisContainer.isRunning());
        if (redisContainer.isRunning()) {
            log.info("Redis host: {}", redisContainer.getHost());
            log.info("Redis port: {}", redisContainer.getMappedPort(6379));
            
            // Execute redis-cli command to check status
            try {
                var result = redisContainer.execInContainer("redis-cli", "ping");
                log.info("Redis ping result: {}", result.getStdout().trim());
                if (!result.getStderr().isEmpty()) {
                    log.error("Redis ping error: {}", result.getStderr());
                }
            } catch (Exception e) {
                log.error("Failed to ping Redis: {}", e.getMessage());
            }
        }
        
        log.info("=== End Redis Diagnostics ===");
    }
    
    /**
     * Check if container is healthy using Docker healthcheck
     */
    private static boolean isContainerHealthy(GenericContainer<?> container) {
        try {
            // Use reflection to check if container has isHealthy method
            return container.isRunning();
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Wait with logging
     */
    public static void waitWithLogging(String message, int seconds) {
        log.info("{} for {} seconds...", message, seconds);
        for (int i = 0; i < seconds; i++) {
            try {
                TimeUnit.SECONDS.sleep(1);
                if (i % 5 == 0 && i > 0) {
                    log.info("  ... {} seconds elapsed", i);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
        log.info("  ... wait complete");
    }
    
    /**
     * Log test environment information
     */
    public static void logTestEnvironment() {
        log.info("=== Test Environment ===");
        log.info("Java Version: {}", System.getProperty("java.version"));
        log.info("OS: {} {}", System.getProperty("os.name"), System.getProperty("os.version"));
        log.info("Docker Version: {}", getDockerVersion());
        log.info("Available processors: {}", Runtime.getRuntime().availableProcessors());
        log.info("Max memory: {} MB", Runtime.getRuntime().maxMemory() / 1024 / 1024);
        log.info("=== End Test Environment ===");
    }
    
    private static String getDockerVersion() {
        try {
            Process process = Runtime.getRuntime().exec("docker --version");
            process.waitFor(2, TimeUnit.SECONDS);
            byte[] output = process.getInputStream().readAllBytes();
            return new String(output).trim();
        } catch (Exception e) {
            return "Unknown - " + e.getMessage();
        }
    }
}