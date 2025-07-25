package com.enrollment.e2e.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.ComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.io.File;
import java.time.Duration;

/**
 * TestContainers configuration that uses Docker Compose to start all services.
 * This provides true end-to-end testing with isolated containers.
 */
@TestConfiguration
public class DockerComposeTestConfig {

    @Bean
    public ComposeContainer composeContainer() {
        return new ComposeContainer(new File("../docker-compose.test.yml"))
                .withExposedService("mongodb", 27017)
                .withExposedService("redis", 6379)
                .withExposedService("eureka", 8761, 
                    Wait.forHttp("/actuator/health").withStartupTimeout(Duration.ofSeconds(120)))
                .withExposedService("auth-service", 3001,
                    Wait.forHttp("/actuator/health").withStartupTimeout(Duration.ofSeconds(120)))
                .withExposedService("course-service", 3002,
                    Wait.forHttp("/actuator/health").withStartupTimeout(Duration.ofSeconds(120)))
                .withExposedService("enrollment-service", 3003,
                    Wait.forHttp("/actuator/health").withStartupTimeout(Duration.ofSeconds(120)))
                .withExposedService("grade-service", 3004,
                    Wait.forHttp("/actuator/health").withStartupTimeout(Duration.ofSeconds(120)))
                .withLocalCompose(true)
                .withPull(false); // Don't pull images, use local builds
    }
}