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
 * Tracks user registrations to store role information for login
 */
public class RegistrationTracker extends ResponseDefinitionTransformer {
    
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    @Override
    public ResponseDefinition transform(Request request, ResponseDefinition responseDefinition, 
                                       FileSource files, Parameters parameters) {
        
        // Only apply to successful registration
        if (!request.getUrl().equals("/api/auth/register") || 
            !request.getMethod().toString().equals("POST") ||
            responseDefinition.getStatus() != 201) {
            return responseDefinition;
        }
        
        try {
            JsonNode requestBody = objectMapper.readTree(request.getBodyAsString());
            String email = requestBody.get("email").asText();
            String role = requestBody.get("role").asText();
            
            // Store the user's role for login
            LoginTokenGenerator.storeUserRole(email, role);
            System.out.println("RegistrationTracker: Stored role for email=" + email + ", role=" + role);
            
        } catch (Exception e) {
            // Ignore parsing errors
        }
        
        return responseDefinition;
    }
    
    @Override
    public String getName() {
        return "registration-tracker";
    }
    
    @Override
    public boolean applyGlobally() {
        return false;
    }
}