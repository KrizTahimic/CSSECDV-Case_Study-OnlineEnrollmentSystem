package com.enrollment.course.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.mongodb.core.MongoTemplate;
import com.enrollment.course.repository.CourseRepository;
import com.enrollment.course.client.AuthClient;

@TestConfiguration
public class TestConfig {
    
    @MockBean
    private MongoTemplate mongoTemplate;
    
    @MockBean
    private CourseRepository courseRepository;
    
    @MockBean
    private AuthClient authClient;
}