package com.enrollment.e2e.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.common.FileSource;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.extension.ResponseDefinitionTransformer;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;

/**
 * Tracks course capacity when courses are created
 */
public class CourseCapacityTracker extends ResponseDefinitionTransformer {
    
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    @Override
    public ResponseDefinition transform(Request request, ResponseDefinition responseDefinition, 
                                       FileSource files, Parameters parameters) {
        
        // Only apply to course creation
        if (!request.getUrl().equals("/api/courses") || 
            !request.getMethod().toString().equals("POST") ||
            responseDefinition.getStatus() != 201) {
            return responseDefinition;
        }
        
        try {
            JsonNode requestBody = objectMapper.readTree(request.getBodyAsString());
            
            // Extract capacity if provided
            if (requestBody.has("capacity")) {
                int capacity = requestBody.get("capacity").asInt();
                
                // Extract course ID from response
                String responseBody = responseDefinition.getBody();
                JsonNode responseJson = objectMapper.readTree(responseBody);
                String courseId = responseJson.get("id").asText();
                
                // Track the capacity
                EnrollmentCapacityChecker.setCourseCapacity(courseId, capacity);
            }
            
        } catch (Exception e) {
            // Ignore errors
        }
        
        return responseDefinition;
    }
    
    @Override
    public String getName() {
        return "course-capacity-tracker";
    }
    
    @Override
    public boolean applyGlobally() {
        return true;
    }
}