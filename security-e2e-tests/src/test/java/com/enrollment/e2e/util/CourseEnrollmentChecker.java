package com.enrollment.e2e.util;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Checks JWT role for course enrollment viewing operations
 */
public class CourseEnrollmentChecker extends ResponseDefinitionTransformer {
    
    private static final String SECRET = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    // Track enrollments for courses
    private static final Map<String, List<Map<String, String>>> courseEnrollments = new HashMap<>();
    
    public static void addEnrollment(String courseId, String studentEmail) {
        courseEnrollments.computeIfAbsent(courseId, k -> new ArrayList<>())
            .add(Map.of("studentEmail", studentEmail, "courseId", courseId, "status", "ENROLLED"));
    }
    
    public static void clearEnrollments() {
        courseEnrollments.clear();
    }
    
    private Key getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(SECRET);
        return Keys.hmacShaKeyFor(keyBytes);
    }
    
    @Override
    public ResponseDefinition transform(Request request, ResponseDefinition responseDefinition, 
                                       FileSource files, Parameters parameters) {
        
        // Apply to course enrollment viewing
        if (!request.getUrl().matches("/api/enrollments/course/[a-zA-Z0-9-]+") || 
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
        String courseId = request.getUrl().replaceAll("/api/enrollments/course/", "");
        
        try {
            Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
            
            String role = claims.get("role", String.class);
            
            // Only faculty and admin can view course enrollments
            if (!"faculty".equals(role) && !"admin".equals(role)) {
                return new ResponseDefinitionBuilder()
                    .withStatus(403)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"error\":\"Access denied\"}")
                    .build();
            }
            
            // Return enrollments for the course
            List<Map<String, String>> enrollments = courseEnrollments.getOrDefault(courseId, new ArrayList<>());
            
            try {
                String json = objectMapper.writeValueAsString(enrollments);
                return new ResponseDefinitionBuilder()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(json)
                    .build();
            } catch (Exception e) {
                return new ResponseDefinitionBuilder()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("[]")
                    .build();
            }
                
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
        return "course-enrollment-checker";
    }
    
    @Override
    public boolean applyGlobally() {
        return true;
    }
}