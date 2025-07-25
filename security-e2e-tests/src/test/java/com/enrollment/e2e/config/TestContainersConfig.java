package com.enrollment.e2e.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Test container configuration for end-to-end security tests.
 * Provides MongoDB and Redis containers for testing.
 */
@TestConfiguration
@Testcontainers
public class TestContainersConfig {

    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer(DockerImageName.parse("mongo:7.0"))
            .withExposedPorts(27017);

    @Container
    static GenericContainer<?> redisContainer = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    static {
        mongoDBContainer.start();
        redisContainer.start();
        
        // Set system properties for all tests
        System.setProperty("spring.data.mongodb.uri", mongoDBContainer.getReplicaSetUrl());
        System.setProperty("spring.redis.host", redisContainer.getHost());
        System.setProperty("spring.redis.port", String.valueOf(redisContainer.getMappedPort(6379)));
    }

    @Bean
    public MongoDBContainer mongoDBContainer() {
        return mongoDBContainer;
    }

    @Bean
    public GenericContainer<?> redisContainer() {
        return redisContainer;
    }
}