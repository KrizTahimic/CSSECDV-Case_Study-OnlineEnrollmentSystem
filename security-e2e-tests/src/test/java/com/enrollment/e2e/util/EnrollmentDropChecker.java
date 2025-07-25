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
import java.util.HashMap;
import java.util.Map;

/**
 * Checks JWT for enrollment drop operations
 */
public class EnrollmentDropChecker extends ResponseDefinitionTransformer {
    
    private static final String SECRET = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    
    // Track which students are enrolled in which courses
    private static final Map<String, String> studentEnrollments = new HashMap<>();
    
    public static void recordEnrollment(String studentEmail, String courseId) {
        studentEnrollments.put(courseId, studentEmail);
    }
    
    public static void clearEnrollments() {
        studentEnrollments.clear();
    }
    
    private Key getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(SECRET);
        return Keys.hmacShaKeyFor(keyBytes);
    }
    
    @Override
    public ResponseDefinition transform(Request request, ResponseDefinition responseDefinition, 
                                       FileSource files, Parameters parameters) {
        
        // Apply to enrollment deletion
        if (!request.getUrl().matches("/api/enrollments/[a-zA-Z0-9-]+") || 
            !request.getMethod().toString().equals("DELETE")) {
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
        String courseId = request.getUrl().replaceAll("/api/enrollments/", "");
        
        try {
            Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
            
            String userEmail = claims.getSubject();
            String role = claims.get("role", String.class);
            
            // Check if this student is enrolled in the course
            String enrolledStudent = studentEnrollments.get(courseId);
            
            // Students can only drop their own enrollments
            if ("student".equals(role) && !userEmail.equals(enrolledStudent)) {
                return new ResponseDefinitionBuilder()
                    .withStatus(403)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"error\":\"Access denied\"}")
                    .build();
            }
            
            // Faculty and admin can drop any enrollment
            // Remove the enrollment
            if (enrolledStudent != null) {
                studentEnrollments.remove(courseId);
                // Also update the enrollment count
                EnrollmentCapacityChecker.recordDrop(courseId);
            }
            
            return new ResponseDefinitionBuilder()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"message\":\"Course dropped successfully\"}")
                .build();
                
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
        return "enrollment-drop-checker";
    }
    
    @Override
    public boolean applyGlobally() {
        return true;
    }
}