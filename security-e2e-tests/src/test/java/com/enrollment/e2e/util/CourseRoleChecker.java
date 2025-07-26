package com.enrollment.e2e.util;

import com.fasterxml.jackson.databind.JsonNode;
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
import java.util.UUID;

/**
 * Checks JWT role for course management operations
 */
public class CourseRoleChecker extends ResponseDefinitionTransformer {
    
    private static final String SECRET = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    private Key getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(SECRET);
        return Keys.hmacShaKeyFor(keyBytes);
    }
    
    @Override
    public ResponseDefinition transform(Request request, ResponseDefinition responseDefinition, 
                                       FileSource files, Parameters parameters) {
        
        // Only apply to course creation endpoint
        if (!request.getUrl().equals("/api/courses") || !request.getMethod().toString().equals("POST")) {
            return responseDefinition;
        }
        
        System.out.println("CourseRoleChecker: Processing course creation request");
        
        String authHeader = request.getHeader("Authorization");
        System.out.println("CourseRoleChecker: Authorization header = " + authHeader);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return new ResponseDefinitionBuilder()
                .withStatus(401)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"error\":\"Authentication required\"}")
                .build();
        }
        
        String token = authHeader.substring(7);
        System.out.println("CourseRoleChecker: Token = " + token);
        
        try {
            Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
            
            String role = claims.get("role", String.class);
            System.out.println("CourseRoleChecker: Token email=" + claims.getSubject() + ", role=" + role);
            
            // Only faculty and admin can create courses
            if ("student".equals(role)) {
                return new ResponseDefinitionBuilder()
                    .withStatus(403)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"error\":\"Access denied\"}")
                    .build();
            }
            
            // Faculty and admin can create courses
            try {
                JsonNode requestBody = objectMapper.readTree(request.getBodyAsString());
                String courseCode = requestBody.has("code") ? requestBody.get("code").asText() : "CS301";
                String courseName = requestBody.has("name") ? requestBody.get("name").asText() : "Advanced Algorithms";
                int capacity = requestBody.has("capacity") ? requestBody.get("capacity").asInt() : 30;
                
                return new ResponseDefinitionBuilder()
                    .withStatus(201)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{" +
                        "\"id\":\"" + UUID.randomUUID().toString() + "\"," +
                        "\"code\":\"" + courseCode + "\"," +
                        "\"name\":\"" + courseName + "\"," +
                        "\"capacity\":" + capacity + "," +
                        "\"enrolled\":0" +
                        "}")
                    .build();
            } catch (Exception e) {
                return new ResponseDefinitionBuilder()
                    .withStatus(201)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{" +
                        "\"id\":\"" + UUID.randomUUID().toString() + "\"," +
                        "\"courseCode\":\"CS301\"," +
                        "\"courseName\":\"Advanced Algorithms\"," +
                        "\"capacity\":30," +
                        "\"enrolled\":0" +
                        "}")
                    .build();
            }
                
        } catch (Exception e) {
            // Invalid token
            System.err.println("CourseRoleChecker: Error parsing token - " + e.getMessage());
            e.printStackTrace();
            return new ResponseDefinitionBuilder()
                .withStatus(403)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"error\":\"Invalid token: " + e.getMessage() + "\"}")
                .build();
        }
    }
    
    @Override
    public String getName() {
        return "course-role-checker";
    }
    
    @Override
    public boolean applyGlobally() {
        return false;
    }
}