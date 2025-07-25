package com.enrollment.e2e.util;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.common.FileSource;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.extension.ResponseDefinitionTransformer;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles password change logic with re-authentication check
 */
public class PasswordChangeTransformer extends ResponseDefinitionTransformer {
    
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final Set<String> reauthenticatedTokens = ConcurrentHashMap.newKeySet();
    
    @Override
    public ResponseDefinition transform(Request request, ResponseDefinition responseDefinition, 
                                       FileSource files, Parameters parameters) {
        
        if (!request.getUrl().equals("/api/auth/change-password")) {
            return responseDefinition;
        }
        
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return responseDefinition;
        }
        
        String token = authHeader.substring(7);
        
        try {
            JsonNode body = objectMapper.readTree(request.getBodyAsString());
            String currentPassword = body.get("currentPassword").asText();
            String newPassword = body.get("newPassword").asText();
            
            // Check if re-authenticated
            if (!reauthenticatedTokens.contains(token)) {
                return new ResponseDefinitionBuilder()
                    .withStatus(403)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"error\":\"Re-authentication required for sensitive operations\"}")
                    .build();
            }
            
            // If re-authenticated and password is SecurePass123!, allow change
            if (currentPassword.equals("SecurePass123!") && newPassword.equals("NewSecurePass123!")) {
                reauthenticatedTokens.remove(token); // Clear after use
                return new ResponseDefinitionBuilder()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"message\":\"Password changed successfully\"}")
                    .build();
            }
            
        } catch (Exception e) {
            // Fall through to default response
        }
        
        return responseDefinition;
    }
    
    @Override
    public String getName() {
        return "password-change-transformer";
    }
    
    @Override
    public boolean applyGlobally() {
        return false;
    }
    
    public static void markReauthenticated(String token) {
        reauthenticatedTokens.add(token);
    }
    
    public static void reset() {
        reauthenticatedTokens.clear();
    }
}