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
 * Checks JWT role for enrollment access operations
 */
public class EnrollmentRoleChecker extends ResponseDefinitionTransformer {
    
    private static final String SECRET = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    
    static {
        System.out.println("EnrollmentRoleChecker class loaded");
    }
    
    public EnrollmentRoleChecker() {
        System.out.println("EnrollmentRoleChecker instance created");
    }
    
    private Key getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(SECRET);
        return Keys.hmacShaKeyFor(keyBytes);
    }
    
    @Override
    public ResponseDefinition transform(Request request, ResponseDefinition responseDefinition, 
                                       FileSource files, Parameters parameters) {
        
        System.out.println("EnrollmentRoleChecker: transform called for " + request.getUrl() + " method=" + request.getMethod());
        
        // Apply to student enrollment viewing
        if (!request.getUrl().matches("/api/enrollments/student/[a-zA-Z0-9@.%-]+") || 
            !request.getMethod().toString().equals("GET")) {
            return responseDefinition;
        }
        
        String authHeader = request.getHeader("Authorization");
        System.out.println("EnrollmentRoleChecker: URL=" + request.getUrl() + ", Auth header=" + authHeader);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            System.out.println("EnrollmentRoleChecker: Returning 401 - no valid auth header");
            return new ResponseDefinitionBuilder()
                .withStatus(401)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"error\":\"Authentication required\"}")
                .build();
        }
        
        String token = authHeader.substring(7);
        String requestedEmail = request.getUrl().replaceAll("/api/enrollments/student/", "");
        // Decode URL-encoded email
        try {
            requestedEmail = java.net.URLDecoder.decode(requestedEmail, "UTF-8");
        } catch (Exception e) {
            // Use as-is if decode fails
        }
        
        try {
            Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
            
            String userEmail = claims.getSubject();
            String role = claims.get("role", String.class);
            
            System.out.println("EnrollmentRoleChecker: userEmail=" + userEmail + ", requestedEmail=" + requestedEmail + ", role=" + role);
            
            // Students can only view their own enrollments
            if ("student".equals(role) && !userEmail.equals(requestedEmail)) {
                System.out.println("EnrollmentRoleChecker: Student trying to view another student's enrollments - denied");
                return new ResponseDefinitionBuilder()
                    .withStatus(403)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"error\":\"Access denied\"}")
                    .build();
            }
            
            // Faculty and admin can view any student's enrollments
            System.out.println("EnrollmentRoleChecker: Access granted, returning original response");
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
        return "enrollment-role-checker";
    }
    
    @Override
    public boolean applyGlobally() {
        return true;
    }
}