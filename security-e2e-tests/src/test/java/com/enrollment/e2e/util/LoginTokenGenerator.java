package com.enrollment.e2e.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.common.FileSource;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.extension.ResponseDefinitionTransformer;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Generates JWT tokens based on user registration data for login endpoints
 */
public class LoginTokenGenerator extends ResponseDefinitionTransformer {
    
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final Map<String, String> userRoles = new ConcurrentHashMap<>();
    
    static {
        System.out.println("LoginTokenGenerator class loaded");
    }
    
    public LoginTokenGenerator() {
        System.out.println("LoginTokenGenerator instance created");
    }
    
    @Override
    public ResponseDefinition transform(Request request, ResponseDefinition responseDefinition, 
                                       FileSource files, Parameters parameters) {
        
        // Only apply to login endpoint
        if (!request.getUrl().equals("/api/auth/login") || 
            !request.getMethod().toString().equals("POST")) {
            return responseDefinition;
        }
        
        // Only process successful responses
        if (responseDefinition.getStatus() != 200) {
            return responseDefinition;
        }
        
        try {
            JsonNode requestBody = objectMapper.readTree(request.getBodyAsString());
            String email = requestBody.get("email").asText();
            String password = requestBody.get("password").asText();
            
            // Check if password is correct
            if (!"SecurePass123!".equals(password)) {
                return new ResponseDefinitionBuilder()
                    .withStatus(401)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"error\":\"Invalid username and/or password\"}")
                    .build();
            }
            
            // Determine role based on email pattern or stored registration data
            String role = determineRole(email);
            
            // Generate token with correct role
            String token = JwtTestUtil.generateToken(email, role);
            
            return new ResponseDefinitionBuilder()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{" +
                    "\"token\":\"" + token + "\"," +
                    "\"email\":\"" + email + "\"," +
                    "\"role\":\"" + role + "\"," +
                    "\"lastLoginTime\":null," +
                    "\"lastLoginIP\":null" +
                    "}")
                .build();
                
        } catch (Exception e) {
            System.err.println("LoginTokenGenerator error: " + e.getMessage());
            e.printStackTrace();
            return new ResponseDefinitionBuilder()
                .withStatus(401)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"error\":\"Invalid username and/or password\"}")
                .build();
        }
    }
    
    private String determineRole(String email) {
        // First check if we have stored role from registration
        String storedRole = userRoles.get(email);
        if (storedRole != null) {
            return storedRole;
        }
        
        // Otherwise determine by email pattern
        if (email.contains("admin")) {
            return "admin";
        } else if (email.contains("faculty")) {
            return "faculty";
        } else {
            return "student";
        }
    }
    
    // Called during registration to store user roles
    public static void storeUserRole(String email, String role) {
        userRoles.put(email, role);
    }
    
    // Clear stored roles for test cleanup
    public static void clearUserRoles() {
        userRoles.clear();
    }
    
    @Override
    public String getName() {
        return "login-token-generator";
    }
    
    @Override
    public boolean applyGlobally() {
        return true;
    }
}