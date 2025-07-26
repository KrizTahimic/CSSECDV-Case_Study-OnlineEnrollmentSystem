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

/**
 * Checks JWT role to enforce security policies
 */
public class JwtRoleChecker extends ResponseDefinitionTransformer {
    
    private static final String SECRET = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    private Key getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(SECRET);
        return Keys.hmacShaKeyFor(keyBytes);
    }
    
    @Override
    public ResponseDefinition transform(Request request, ResponseDefinition responseDefinition, 
                                       FileSource files, Parameters parameters) {
        
        // Only apply to grade submission endpoint
        if (!request.getUrl().equals("/api/grades") || !request.getMethod().toString().equals("POST")) {
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
        
        try {
            Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
            
            String role = claims.get("role", String.class);
            
            // Students should not be able to submit grades
            if ("student".equals(role)) {
                return new ResponseDefinitionBuilder()
                    .withStatus(403)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"error\":\"Access denied\"}")
                    .build();
            }
            
            // Faculty and admin can submit grades
            try {
                JsonNode requestBody = objectMapper.readTree(request.getBodyAsString());
                String studentEmail = requestBody.has("studentEmail") ? requestBody.get("studentEmail").asText() : "student@test.com";
                String courseId = requestBody.has("courseId") ? requestBody.get("courseId").asText() : "course123";
                double score = requestBody.has("score") ? requestBody.get("score").asDouble() : 85.0;
                
                // Calculate letter grade based on score
                String letterGrade;
                if (score >= 90) letterGrade = "A";
                else if (score >= 80) letterGrade = "B";
                else if (score >= 70) letterGrade = "C";
                else if (score >= 60) letterGrade = "D";
                else letterGrade = "F";
                
                return new ResponseDefinitionBuilder()
                    .withStatus(201)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{" +
                        "\"id\":\"" + java.util.UUID.randomUUID().toString() + "\"," +
                        "\"studentEmail\":\"" + studentEmail + "\"," +
                        "\"courseId\":\"" + courseId + "\"," +
                        "\"score\":" + score + "," +
                        "\"letterGrade\":\"" + letterGrade + "\"," +
                        "\"grade\":\"" + letterGrade + "\"" +
                        "}")
                    .build();
            } catch (Exception e) {
                return new ResponseDefinitionBuilder()
                    .withStatus(201)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{" +
                        "\"id\":\"" + java.util.UUID.randomUUID().toString() + "\"," +
                        "\"studentEmail\":\"student@test.com\"," +
                        "\"courseId\":\"course123\"," +
                        "\"score\":85.0," +
                        "\"letterGrade\":\"B\"," +
                        "\"grade\":\"B\"" +
                        "}")
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
        return "jwt-role-checker";
    }
    
    @Override
    public boolean applyGlobally() {
        return false;
    }
}