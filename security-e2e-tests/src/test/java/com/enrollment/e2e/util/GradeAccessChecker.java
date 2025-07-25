package com.enrollment.e2e.util;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.common.FileSource;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.extension.ResponseDefinitionTransformer;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import java.security.Key;

/**
 * Checks JWT role for grade access operations
 */
public class GradeAccessChecker extends ResponseDefinitionTransformer {
    
    private static final String SECRET = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    
    private Key getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(SECRET);
        return Keys.hmacShaKeyFor(keyBytes);
    }
    
    @Override
    public ResponseDefinition transform(Request request, ResponseDefinition responseDefinition, 
                                       FileSource files, Parameters parameters) {
        
        // Apply to student grade viewing
        if (!request.getUrl().matches("/api/grades/student/[a-zA-Z0-9@.-]+") || 
            !request.getMethod().toString().equals("GET")) {
            return responseDefinition;
        }
        
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return new ResponseDefinitionBuilder()
                .withStatus(401)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"error\":\"Authentication required\"}")
                .build();
        }
        
        String token = authHeader.substring(7);
        String requestedEmail = request.getUrl().replaceAll("/api/grades/student/", "");
        
        try {
            Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
            
            String userEmail = claims.getSubject();
            String role = claims.get("role", String.class);
            
            // Students can only view their own grades
            if ("student".equals(role) && !userEmail.equals(requestedEmail)) {
                return new ResponseDefinitionBuilder()
                    .withStatus(403)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"error\":\"Access denied\"}")
                    .build();
            }
            
            // Faculty and admin can view any student's grades
            return responseDefinition;
                
        } catch (Exception e) {
            // Invalid token
            return new ResponseDefinitionBuilder()
                .withStatus(403)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"error\":\"Invalid token\"}")
                .build();
        }
    }
    
    @Override
    public String getName() {
        return "grade-access-checker";
    }
    
    @Override
    public boolean applyGlobally() {
        return false;
    }
}