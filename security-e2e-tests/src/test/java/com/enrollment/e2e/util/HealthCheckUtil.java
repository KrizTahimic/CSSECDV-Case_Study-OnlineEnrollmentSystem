package com.enrollment.e2e.util;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

/**
 * Utility class for performing health checks on services with retry logic
 */
public class HealthCheckUtil {
    
    private static final Logger log = LoggerFactory.getLogger(HealthCheckUtil.class);
    private static final int MAX_RETRIES = 30;
    private static final long RETRY_DELAY_MS = 2000;
    private static final Duration TIMEOUT = Duration.ofMinutes(2);
    
    /**
     * Wait for a service to be healthy by checking its health endpoint
     * 
     * @param serviceName Name of the service for logging
     * @param healthUrl Full URL to the health endpoint
     * @return true if service becomes healthy within timeout, false otherwise
     */
    public static boolean waitForServiceHealth(String serviceName, String healthUrl) {
        log.info("Waiting for {} to be healthy at: {}", serviceName, healthUrl);
        Instant startTime = Instant.now();
        int attempt = 0;
        
        while (attempt < MAX_RETRIES && Duration.between(startTime, Instant.now()).compareTo(TIMEOUT) < 0) {
            attempt++;
            try {
                Response response = RestAssured
                    .given()
                        .relaxedHTTPSValidation()
                        .baseUri(healthUrl)
                    .when()
                        .get()
                    .then()
                        .extract()
                        .response();
                
                int statusCode = response.getStatusCode();
                if (statusCode == 200) {
                    String body = response.getBody().asString();
                    if (body.contains("UP") || body.contains("\"status\":\"UP\"")) {
                        log.info("{} is healthy after {} attempts", serviceName, attempt);
                        return true;
                    } else {
                        log.debug("{} health check returned 200 but status not UP: {}", serviceName, body);
                    }
                } else {
                    log.debug("{} health check attempt {} failed with status: {}", serviceName, attempt, statusCode);
                }
            } catch (Exception e) {
                log.debug("{} health check attempt {} failed: {}", serviceName, attempt, e.getMessage());
            }
            
            try {
                TimeUnit.MILLISECONDS.sleep(RETRY_DELAY_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        
        log.error("{} did not become healthy after {} attempts in {} seconds", 
            serviceName, attempt, Duration.between(startTime, Instant.now()).getSeconds());
        return false;
    }
    
    /**
     * Check if a service endpoint is accessible (not necessarily healthy)
     * 
     * @param serviceName Name of the service for logging
     * @param url URL to check
     * @return true if endpoint responds with any HTTP status, false if connection refused
     */
    public static boolean isServiceAccessible(String serviceName, String url) {
        try {
            Response response = RestAssured
                .given()
                    .relaxedHTTPSValidation()
                    .baseUri(url)
                .when()
                    .get()
                .then()
                    .extract()
                    .response();
            
            log.debug("{} is accessible at {} with status: {}", serviceName, url, response.getStatusCode());
            return true;
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("Connection refused")) {
                log.debug("{} is not accessible at {}: Connection refused", serviceName, url);
            } else {
                log.debug("{} accessibility check failed: {}", serviceName, e.getMessage());
            }
            return false;
        }
    }
    
    /**
     * Wait for Eureka service registry to have a specific number of registered instances
     * 
     * @param eurekaUrl Eureka base URL
     * @param expectedInstances Expected number of service instances
     * @param timeout Timeout duration
     * @return true if expected instances are registered within timeout
     */
    public static boolean waitForEurekaRegistrations(String eurekaUrl, int expectedInstances, Duration timeout) {
        log.info("Waiting for {} services to register with Eureka", expectedInstances);
        Instant startTime = Instant.now();
        
        while (Duration.between(startTime, Instant.now()).compareTo(timeout) < 0) {
            try {
                Response response = RestAssured
                    .given()
                        .relaxedHTTPSValidation()
                        .baseUri(eurekaUrl)
                    .when()
                        .get("/eureka/apps")
                    .then()
                        .extract()
                        .response();
                
                if (response.getStatusCode() == 200) {
                    String body = response.getBody().asString();
                    // Count occurrences of <instance> tags in XML response
                    int instanceCount = countOccurrences(body, "<instance>");
                    log.debug("Current Eureka registrations: {}/{}", instanceCount, expectedInstances);
                    
                    if (instanceCount >= expectedInstances) {
                        log.info("All {} services registered with Eureka", instanceCount);
                        return true;
                    }
                }
            } catch (Exception e) {
                log.debug("Eureka registration check failed: {}", e.getMessage());
            }
            
            try {
                TimeUnit.SECONDS.sleep(2);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        
        log.error("Timeout waiting for {} services to register with Eureka", expectedInstances);
        return false;
    }
    
    private static int countOccurrences(String str, String substring) {
        int count = 0;
        int idx = 0;
        while ((idx = str.indexOf(substring, idx)) != -1) {
            count++;
            idx += substring.length();
        }
        return count;
    }
}