package com.enrollment.grade.controller;

import com.enrollment.grade.model.Grade;
import com.enrollment.grade.repository.GradeRepository;
import com.enrollment.grade.service.GradeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.security.Key;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for GradeController with full Spring context.
 * Tests controller behavior with actual security configuration and role-based access.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "jwt.secret=dGVzdHNlY3JldDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDEyMzQ1Njc4OTA=",
    "jwt.expiration=86400000",
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration,org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration",
    "eureka.client.enabled=false",
    "spring.cloud.discovery.enabled=false"
})
class GradeControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private GradeService gradeService;
    
    @MockBean
    private GradeRepository gradeRepository;
    
    @MockBean
    private DiscoveryClient discoveryClient;

    private Grade sampleGrade;
    private String studentJwtToken;
    private String facultyJwtToken;
    private String adminJwtToken;

    @BeforeEach
    void setUp() {
        // Setup sample grade
        sampleGrade = new Grade();
        sampleGrade.setId("grade123");
        sampleGrade.setStudentEmail("student@test.com");
        sampleGrade.setCourseId("course123");
        sampleGrade.setScore(85.5);
        sampleGrade.setLetterGrade("B");
        sampleGrade.setSubmissionDate(LocalDateTime.now());
        sampleGrade.setFacultyId("faculty@test.com");
        sampleGrade.setComments("Good work");

        // Create JWT tokens for different roles
        String secretKey = "dGVzdHNlY3JldDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDEyMzQ1Njc4OTA=";
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        Key key = Keys.hmacShaKeyFor(keyBytes);
        
        // Student token
        String studentToken = Jwts.builder()
                .setSubject("student@test.com")
                .claim("role", "student")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 86400000))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
        studentJwtToken = "Bearer " + studentToken;

        // Faculty token
        String facultyToken = Jwts.builder()
                .setSubject("faculty@test.com")
                .claim("role", "faculty")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 86400000))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
        facultyJwtToken = "Bearer " + facultyToken;

        // Admin token
        String adminToken = Jwts.builder()
                .setSubject("admin@test.com")
                .claim("role", "admin")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 86400000))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
        adminJwtToken = "Bearer " + adminToken;
    }

    @Test
    @DisplayName("Should require authentication for all grade endpoints")
    void shouldRequireAuthenticationForAllEndpoints() throws Exception {
        // Without authentication, all requests should return 401
        mockMvc.perform(get("/api/grades"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/grades/student/student@test.com"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/grades/course/course123"))
                .andExpect(status().isUnauthorized());

        Map<String, Object> request = new HashMap<>();
        request.put("studentEmail", "student@test.com");
        request.put("courseId", "course123");
        request.put("score", 85.0);

        mockMvc.perform(post("/api/grades")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(put("/api/grades/grade123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(delete("/api/grades/grade123"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should allow students to view their own grades")
    @WithMockUser(username = "student@test.com", roles = "STUDENT")
    void shouldAllowStudentsToViewOwnGrades() throws Exception {
        List<Grade> grades = Arrays.asList(sampleGrade);
        when(gradeService.getStudentGrades("student@test.com")).thenReturn(grades);

        mockMvc.perform(get("/api/grades/student/student@test.com")
                .header("Authorization", studentJwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("grade123"));

        verify(gradeService).getStudentGrades("student@test.com");
    }

    @Test
    @DisplayName("Should prevent students from viewing other students' grades")
    @WithMockUser(username = "student1@test.com", roles = "STUDENT")
    void shouldPreventStudentsFromViewingOthersGrades() throws Exception {
        mockMvc.perform(get("/api/grades/student/student2@test.com"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should prevent students from viewing all grades")
    @WithMockUser(roles = "STUDENT")
    void shouldPreventStudentsFromViewingAllGrades() throws Exception {
        mockMvc.perform(get("/api/grades"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should allow faculty to view all grades")
    @WithMockUser(roles = "FACULTY")
    void shouldAllowFacultyToViewAllGrades() throws Exception {
        List<Grade> grades = Arrays.asList(sampleGrade);
        when(gradeService.getAllGrades()).thenReturn(grades);

        mockMvc.perform(get("/api/grades"))
                .andExpect(status().isOk());

        verify(gradeService).getAllGrades();
    }

    @Test
    @DisplayName("Should allow faculty to view course grades")
    @WithMockUser(roles = "FACULTY")
    void shouldAllowFacultyToViewCourseGrades() throws Exception {
        List<Grade> grades = Arrays.asList(sampleGrade);
        when(gradeService.getCourseGrades("course123")).thenReturn(grades);

        mockMvc.perform(get("/api/grades/course/course123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].courseId").value("course123"));

        verify(gradeService).getCourseGrades("course123");
    }

    @Test
    @DisplayName("Should allow faculty to submit grades")
    @WithMockUser(username = "faculty@test.com", roles = "FACULTY")
    void shouldAllowFacultyToSubmitGrades() throws Exception {
        Grade gradeToSubmit = new Grade();
        gradeToSubmit.setStudentEmail("student@test.com");
        gradeToSubmit.setCourseId("course123");
        gradeToSubmit.setScore(90.0);

        when(gradeService.submitGrade(org.mockito.ArgumentMatchers.any(Grade.class))).thenReturn(sampleGrade);

        mockMvc.perform(post("/api/grades")
                .header("Authorization", facultyJwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(gradeToSubmit)))
                .andExpect(status().isOk());

        verify(gradeService).submitGrade(org.mockito.ArgumentMatchers.argThat(grade -> 
            grade.getFacultyId().equals("faculty@test.com")
        ));
    }

    @Test
    @DisplayName("Should prevent students from submitting grades")
    @WithMockUser(roles = "STUDENT")
    void shouldPreventStudentsFromSubmittingGrades() throws Exception {
        Grade gradeToSubmit = new Grade();
        gradeToSubmit.setStudentEmail("student@test.com");
        gradeToSubmit.setCourseId("course123");
        gradeToSubmit.setScore(90.0);

        mockMvc.perform(post("/api/grades")
                .header("Authorization", studentJwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(gradeToSubmit)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should allow faculty to update grades")
    @WithMockUser(roles = "FACULTY")
    void shouldAllowFacultyToUpdateGrades() throws Exception {
        Grade updatedGrade = new Grade();
        updatedGrade.setScore(95.0);
        updatedGrade.setComments("Excellent work");

        when(gradeService.updateGrade(eq("grade123"), org.mockito.ArgumentMatchers.any(Grade.class))).thenReturn(sampleGrade);

        mockMvc.perform(put("/api/grades/grade123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedGrade)))
                .andExpect(status().isOk());

        verify(gradeService).updateGrade(eq("grade123"), org.mockito.ArgumentMatchers.any(Grade.class));
    }

    @Test
    @DisplayName("Should prevent students from updating grades")
    @WithMockUser(roles = "STUDENT")
    void shouldPreventStudentsFromUpdatingGrades() throws Exception {
        Grade updatedGrade = new Grade();
        updatedGrade.setScore(95.0);

        mockMvc.perform(put("/api/grades/grade123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedGrade)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should allow only admin to delete grades")
    @WithMockUser(roles = "ADMIN")
    void shouldAllowAdminToDeleteGrades() throws Exception {
        doNothing().when(gradeService).deleteGrade("grade123");

        mockMvc.perform(delete("/api/grades/grade123"))
                .andExpect(status().isOk());

        verify(gradeService).deleteGrade("grade123");
    }

    @Test
    @DisplayName("Should prevent faculty from deleting grades")
    @WithMockUser(roles = "FACULTY")
    void shouldPreventFacultyFromDeletingGrades() throws Exception {
        mockMvc.perform(delete("/api/grades/grade123"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should allow students to view specific grade for their course")
    @WithMockUser(username = "student@test.com", roles = "STUDENT")
    void shouldAllowStudentsToViewSpecificGrade() throws Exception {
        when(gradeService.getStudentCourseGrade("student@test.com", "course123"))
                .thenReturn(Optional.of(sampleGrade));

        mockMvc.perform(get("/api/grades/student/student@test.com/course/course123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("grade123"));

        verify(gradeService).getStudentCourseGrade("student@test.com", "course123");
    }

    @Test
    @DisplayName("Should handle grade service exceptions gracefully")
    @WithMockUser(roles = "FACULTY")
    void shouldHandleServiceExceptionsGracefully() throws Exception {
        when(gradeService.updateGrade(eq("grade123"), org.mockito.ArgumentMatchers.any(Grade.class)))
                .thenThrow(new RuntimeException("Grade not found"));

        Grade updatedGrade = new Grade();
        updatedGrade.setScore(95.0);

        mockMvc.perform(put("/api/grades/grade123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedGrade)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should validate authorization with lowercase roles")
    @WithMockUser(roles = "FACULTY")
    void shouldValidateAuthorizationWithLowercaseRoles() throws Exception {
        Grade gradeToSubmit = new Grade();
        gradeToSubmit.setStudentEmail("student@test.com");
        gradeToSubmit.setCourseId("course123");
        gradeToSubmit.setScore(90.0);

        when(gradeService.submitGrade(org.mockito.ArgumentMatchers.any(Grade.class))).thenReturn(sampleGrade);

        // Create faculty JWT token (lowercase)
        String secretKey = "dGVzdHNlY3JldDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDEyMzQ1Njc4OTA=";
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        Key key = Keys.hmacShaKeyFor(keyBytes);
        
        String facultyToken = Jwts.builder()
                .setSubject("faculty@test.com")
                .claim("role", "faculty")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 86400000))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        mockMvc.perform(post("/api/grades")
                .header("Authorization", "Bearer " + facultyToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(gradeToSubmit)))
                .andExpect(status().isOk());
    }
}