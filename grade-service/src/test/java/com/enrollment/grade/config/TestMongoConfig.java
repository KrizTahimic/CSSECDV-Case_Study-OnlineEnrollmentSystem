package com.enrollment.grade.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.repository.support.MongoRepositoryFactoryBean;

import static org.mockito.Mockito.mock;

@TestConfiguration
public class TestMongoConfig {

    @Bean
    public MongoTemplate mongoTemplate() {
        return mock(MongoTemplate.class);
    }
}