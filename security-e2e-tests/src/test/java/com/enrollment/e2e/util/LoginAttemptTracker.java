package com.enrollment.e2e.util;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.common.FileSource;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.extension.ResponseDefinitionTransformer;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import com.jayway.jsonpath.JsonPath;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tracks login attempts for account lockout simulation
 */
public class LoginAttemptTracker extends ResponseDefinitionTransformer {
    
    private static final int MAX_ATTEMPTS = 5;
    private static final Map<String, AtomicInteger> failedAttempts = new ConcurrentHashMap<>();
    
    static {
        // Pre-populate locked account for testing
        failedAttempts.put("locked@test.com", new AtomicInteger(MAX_ATTEMPTS));
    }
    
    @Override
    public ResponseDefinition transform(Request request, ResponseDefinition responseDefinition, 
                                       FileSource files, Parameters parameters) {
        // Only process login requests
        if (!request.getUrl().equals("/api/auth/login")) {
            return responseDefinition;
        }
        
        try {
            String requestBody = request.getBodyAsString();
            String email = JsonPath.read(requestBody, "$.email");
            String password = JsonPath.read(requestBody, "$.password");
            
            // Check if account is locked FIRST
            AtomicInteger attempts = failedAttempts.get(email);
            if (attempts != null && attempts.get() >= MAX_ATTEMPTS) {
                return new ResponseDefinitionBuilder()
                    .withStatus(401)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"error\":\"Account is locked due to multiple failed login attempts\"}")
                    .build();
            }
            
            // Check if password is wrong (not the expected password)
            if (!"SecurePass123!".equals(password)) {
                // This is a failed attempt, increment counter
                failedAttempts.computeIfAbsent(email, k -> new AtomicInteger(0)).incrementAndGet();
                
                // Always return invalid password error for failed attempts
                // The lock will be checked on the NEXT attempt
                return new ResponseDefinitionBuilder()
                    .withStatus(401)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"error\":\"Invalid username and/or password\"}")
                    .build();
            }
            
            // Successful login, reset counter
            failedAttempts.remove(email);
            
            // Let other transformers handle the successful login
            return responseDefinition;
            
        } catch (Exception e) {
            // If parsing fails, return original response
            return responseDefinition;
        }
    }
    
    @Override
    public String getName() {
        return "login-attempt-tracker";
    }
    
    public static void reset() {
        failedAttempts.clear();
    }
}