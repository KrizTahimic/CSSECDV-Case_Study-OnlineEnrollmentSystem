package com.enrollment.e2e.util;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.common.FileSource;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.extension.ResponseDefinitionTransformer;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import java.security.Key;

/**
 * Checks for invalid JWT tokens on GET /api/courses endpoint
 */
public class InvalidTokenChecker extends ResponseDefinitionTransformer {
    
    private static final String SECRET = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    
    private Key getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(SECRET);
        return Keys.hmacShaKeyFor(keyBytes);
    }
    
    @Override
    public ResponseDefinition transform(Request request, ResponseDefinition responseDefinition, 
                                       FileSource files, Parameters parameters) {
        
        // Only apply to GET /api/courses for invalid token testing
        if (!request.getUrl().equals("/api/courses") || 
            !request.getMethod().toString().equals("GET")) {
            return responseDefinition;
        }
        
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return responseDefinition;
        }
        
        String token = authHeader.substring(7);
        
        // Validate JWT signature
        try {
            Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token);
            
            // Token is valid, let the original response through
            return responseDefinition;
            
        } catch (Exception e) {
            // Invalid token signature or format
            return new ResponseDefinitionBuilder()
                .withStatus(403)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"error\":\"Invalid token\"}")
                .build();
        }
    }
    
    @Override
    public String getName() {
        return "invalid-token-checker";
    }
    
    @Override
    public boolean applyGlobally() {
        return false;
    }
}