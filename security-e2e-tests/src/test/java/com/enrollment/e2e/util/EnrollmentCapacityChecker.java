package com.enrollment.e2e.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.common.FileSource;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.extension.ResponseDefinitionTransformer;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Checks course capacity for enrollment operations
 */
public class EnrollmentCapacityChecker extends ResponseDefinitionTransformer {
    
    private static final Map<String, Integer> COURSE_ENROLLMENTS = new ConcurrentHashMap<>();
    private static final Map<String, Integer> COURSE_CAPACITIES = new ConcurrentHashMap<>();
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    @Override
    public ResponseDefinition transform(Request request, ResponseDefinition responseDefinition, 
                                       FileSource files, Parameters parameters) {
        
        // Only apply to enrollment creation
        if (!request.getUrl().equals("/api/enrollments") || 
            !request.getMethod().toString().equals("POST")) {
            return responseDefinition;
        }
        
        try {
            JsonNode requestBody = objectMapper.readTree(request.getBodyAsString());
            String courseId = requestBody.get("courseId").asText();
            
            // Check if this is a course with capacity restrictions
            // Course "2" is marked as full in ServiceMockFactory
            if ("2".equals(courseId)) {
                return new ResponseDefinitionBuilder()
                    .withStatus(400)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"error\":\"Course is full\"}")
                    .build();
            }
            
            // Track enrollment for capacity testing
            Integer capacity = COURSE_CAPACITIES.getOrDefault(courseId, 30);
            Integer currentEnrollments = COURSE_ENROLLMENTS.getOrDefault(courseId, 0);
            
            if (currentEnrollments >= capacity) {
                return new ResponseDefinitionBuilder()
                    .withStatus(400)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"error\":\"Course is full\"}")
                    .build();
            }
            
            // Increment enrollment count
            COURSE_ENROLLMENTS.put(courseId, currentEnrollments + 1);
            
            // Track the enrollment for course enrollment queries
            String studentEmail = requestBody.get("studentEmail").asText();
            CourseEnrollmentChecker.addEnrollment(courseId, studentEmail);
            EnrollmentDropChecker.recordEnrollment(studentEmail, courseId);
            
            // Return success response with template processing
            return new ResponseDefinitionBuilder()
                .withStatus(201)
                .withHeader("Content-Type", "application/json")
                .withBody("{" +
                    "\"id\":\"" + UUID.randomUUID().toString() + "\"," +
                    "\"studentId\":\"" + requestBody.get("studentId").asText() + "\"," +
                    "\"studentEmail\":\"" + studentEmail + "\"," +
                    "\"courseId\":\"" + courseId + "\"," +
                    "\"status\":\"ENROLLED\"" +
                    "}")
                .build();
                
        } catch (Exception e) {
            return responseDefinition;
        }
    }
    
    @Override
    public String getName() {
        return "enrollment-capacity-checker";
    }
    
    @Override
    public boolean applyGlobally() {
        return false;
    }
    
    // Helper method to set course capacity for testing
    public static void setCourseCapacity(String courseId, int capacity) {
        COURSE_CAPACITIES.put(courseId, capacity);
    }
    
    // Helper method to reset enrollments for testing
    public static void resetEnrollments() {
        COURSE_ENROLLMENTS.clear();
        COURSE_CAPACITIES.clear();
    }
    
    // Helper method to track enrollments when created
    public static void recordEnrollment(String courseId) {
        COURSE_ENROLLMENTS.put(courseId, COURSE_ENROLLMENTS.getOrDefault(courseId, 0) + 1);
    }
    
    // Helper method to track course drop
    public static void recordDrop(String courseId) {
        Integer current = COURSE_ENROLLMENTS.getOrDefault(courseId, 0);
        if (current > 0) {
            COURSE_ENROLLMENTS.put(courseId, current - 1);
        }
    }
}