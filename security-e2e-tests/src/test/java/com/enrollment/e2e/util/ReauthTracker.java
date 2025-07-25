package com.enrollment.e2e.util;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.common.FileSource;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.extension.ResponseDefinitionTransformer;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks re-authentication state for password changes
 */
public class ReauthTracker extends ResponseDefinitionTransformer {
    
    private static final Set<String> reauthenticatedTokens = ConcurrentHashMap.newKeySet();
    
    @Override
    public ResponseDefinition transform(Request request, ResponseDefinition responseDefinition, 
                                       FileSource files, Parameters parameters) {
        String authHeader = request.getHeader("Authorization");
        
        // Track successful re-authentication
        if (request.getUrl().equals("/api/auth/reauthenticate") && responseDefinition.getStatus() == 200) {
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                reauthenticatedTokens.add(token);
                // Also notify PasswordChangeTransformer
                PasswordChangeTransformer.markReauthenticated(token);
            }
            return responseDefinition;
        }
        
        // Check if password change requires re-authentication
        if (request.getUrl().equals("/api/auth/change-password")) {
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                if (!reauthenticatedTokens.contains(token)) {
                    // Return 403 if not re-authenticated
                    return new ResponseDefinitionBuilder()
                        .withStatus(403)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\":\"Re-authentication required for sensitive operations\"}")
                        .build();
                }
                // Clear the token after password change is processed
                if (responseDefinition.getStatus() == 200) {
                    reauthenticatedTokens.remove(token);
                }
            }
        }
        
        return responseDefinition;
    }
    
    @Override
    public String getName() {
        return "reauth-tracker";
    }
    
    @Override
    public boolean applyGlobally() {
        return true;
    }
    
    public static void reset() {
        reauthenticatedTokens.clear();
    }
}