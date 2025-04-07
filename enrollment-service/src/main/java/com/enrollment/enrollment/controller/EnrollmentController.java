package com.enrollment.enrollment.controller;

import com.enrollment.enrollment.model.Enrollment;
import com.enrollment.enrollment.service.EnrollmentService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Key;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/enrollments")
@RequiredArgsConstructor
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @GetMapping
    public ResponseEntity<List<Enrollment>> getStudentEnrollmentsFromToken(@RequestHeader("Authorization") String authHeader) {
        try {
            // Extract studentId from JWT token
            String token = authHeader.substring(7); // Remove "Bearer " prefix
            String studentId = extractStudentId(token);
            
            return ResponseEntity.ok(enrollmentService.getStudentEnrollments(studentId));
        } catch (Exception e) {
            e.printStackTrace(); // Add this for debugging
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/student/{studentId}")
    public ResponseEntity<List<Enrollment>> getStudentEnrollments(@PathVariable String studentId) {
        return ResponseEntity.ok(enrollmentService.getStudentEnrollments(studentId));
    }

    @GetMapping("/course/{courseId}")
    public ResponseEntity<List<Enrollment>> getCourseEnrollments(@PathVariable String courseId) {
        return ResponseEntity.ok(enrollmentService.getCourseEnrollments(courseId));
    }
    
    @PostMapping
    public ResponseEntity<Enrollment> enrollStudentFromRequest(@RequestHeader("Authorization") String authHeader, @RequestBody Map<String, Object> request) {
        try {
            // Extract courseId from request body
            String courseId = request.get("courseId").toString();
            
            // Extract studentId from JWT token
            String token = authHeader.substring(7); // Remove "Bearer " prefix
            String studentId = extractStudentId(token);
            
            return ResponseEntity.ok(enrollmentService.enrollStudent(studentId, courseId));
        } catch (Exception e) {
            e.printStackTrace(); // Add this for debugging
            return ResponseEntity.badRequest().build();
        }
    }

    private String extractStudentId(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
        System.out.println("JWT Claims: " + claims.toString());
        
        String userId = claims.get("id", String.class);
        if (userId == null) {
            throw new RuntimeException("User ID not found in JWT token");
        }
        System.out.println("Using user ID from token: " + userId);
        return userId;
    }

    private Key getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    @PostMapping("/student/{studentId}/course/{courseId}")
    public ResponseEntity<Enrollment> enrollStudent(
            @PathVariable String studentId,
            @PathVariable String courseId) {
        try {
            return ResponseEntity.ok(enrollmentService.enrollStudent(studentId, courseId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/student/{studentId}/course/{courseId}")
    public ResponseEntity<Void> unenrollStudent(
            @PathVariable String studentId,
            @PathVariable String courseId) {
        try {
            System.out.println("Received drop request for student: " + studentId + " and course: " + courseId);
            enrollmentService.unenrollStudent(studentId, courseId);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            System.err.println("Error in unenrollStudent controller: " + e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
} 