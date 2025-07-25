package com.enrollment.e2e.util;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.common.FileSource;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.extension.ResponseDefinitionTransformer;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import com.jayway.jsonpath.JsonPath;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks login history for lastLoginTime and lastLoginIP
 */
public class LoginTracker extends ResponseDefinitionTransformer {
    
    private static final Map<String, LoginInfo> loginHistory = new ConcurrentHashMap<>();
    
    private static class LoginInfo {
        String lastLoginTime;
        String lastLoginIP;
        
        LoginInfo(String time, String ip) {
            this.lastLoginTime = time;
            this.lastLoginIP = ip;
        }
    }
    
    @Override
    public ResponseDefinition transform(Request request, ResponseDefinition responseDefinition, 
                                       FileSource files, Parameters parameters) {
        // Only process successful login requests
        if (!request.getUrl().equals("/api/auth/login") || responseDefinition.getStatus() != 200) {
            return responseDefinition;
        }
        
        try {
            String requestBody = request.getBodyAsString();
            String email = JsonPath.read(requestBody, "$.email");
            
            // Get previous login info
            LoginInfo previousLogin = loginHistory.get(email);
            
            // Update the response body with previous login info
            String responseBody = responseDefinition.getBody();
            if (previousLogin != null) {
                responseBody = responseBody
                    .replace("\"lastLoginTime\":null", "\"lastLoginTime\":\"" + previousLogin.lastLoginTime + "\"")
                    .replace("\"lastLoginIP\":null", "\"lastLoginIP\":\"" + previousLogin.lastLoginIP + "\"");
            }
            
            // Store current login info for next time
            String currentTime = Instant.now().toString();
            String currentIP = request.getHeader("X-Forwarded-For");
            if (currentIP == null) {
                currentIP = "127.0.0.1";
            }
            loginHistory.put(email, new LoginInfo(currentTime, currentIP));
            
            return new ResponseDefinitionBuilder()
                .withStatus(responseDefinition.getStatus())
                .withHeaders(responseDefinition.getHeaders())
                .withBody(responseBody)
                .build();
            
        } catch (Exception e) {
            // If parsing fails, return original response
            return responseDefinition;
        }
    }
    
    @Override
    public String getName() {
        return "login-tracker";
    }
    
    public static void reset() {
        loginHistory.clear();
    }
}