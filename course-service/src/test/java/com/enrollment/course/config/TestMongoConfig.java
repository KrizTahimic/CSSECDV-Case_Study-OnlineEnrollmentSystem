package com.enrollment.course.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Test configuration that disables the DataLoader to prevent MongoDB connection attempts during tests.
 */
@TestConfiguration
public class TestMongoConfig {

    @Bean
    @Primary
    public CommandLineRunner testDataLoader() {
        // Return a no-op CommandLineRunner to override the DataLoader
        return args -> {
            // Do nothing - prevents MongoDB connection attempts during tests
        };
    }
}