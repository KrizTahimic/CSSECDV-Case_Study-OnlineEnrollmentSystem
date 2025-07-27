package com.enrollment.grade.security;

import com.enrollment.grade.model.Grade;
import com.enrollment.grade.repository.GradeRepository;
import com.enrollment.grade.service.GradeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Security tests for Grade Service.
 * Verifies that all endpoints require proper authentication.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "jwt.secret=testsecret123456789012345678901234567890",
    "spring.security.debug=true",
    "eureka.client.enabled=false",
    "spring.cloud.discovery.enabled=false"
})
class GradeSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private GradeRepository gradeRepository;
    
    @MockBean
    private DiscoveryClient discoveryClient;

    @MockBean
    private GradeService gradeService;

    private Map<String, Object> gradeRequest;

    @BeforeEach
    void setUp() {
        gradeRequest = new HashMap<>();
        gradeRequest.put("studentEmail", "student@test.com");
        gradeRequest.put("courseId", "course123");
        gradeRequest.put("score", 85.0);
    }

    @Test
    @DisplayName("Should require authentication for GET /api/grades")
    void shouldRequireAuthForGetAllGrades() throws Exception {
        mockMvc.perform(get("/api/grades"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should require authentication for GET /api/grades/student/{email}")
    void shouldRequireAuthForGetStudentGrades() throws Exception {
        mockMvc.perform(get("/api/grades/student/student@test.com"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should require authentication for GET /api/grades/course/{id}")
    void shouldRequireAuthForGetCourseGrades() throws Exception {
        mockMvc.perform(get("/api/grades/course/course123"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should require authentication for GET /api/grades/student/{email}/course/{id}")
    void shouldRequireAuthForGetStudentCourseGrade() throws Exception {
        mockMvc.perform(get("/api/grades/student/student@test.com/course/course123"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should require authentication for GET /api/grades/faculty/{id}")
    void shouldRequireAuthForGetFacultyGrades() throws Exception {
        mockMvc.perform(get("/api/grades/faculty/faculty@test.com"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should require authentication for POST /api/grades")
    void shouldRequireAuthForSubmitGrade() throws Exception {
        mockMvc.perform(post("/api/grades")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(gradeRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should require authentication for PUT /api/grades/{id}")
    void shouldRequireAuthForUpdateGrade() throws Exception {
        mockMvc.perform(put("/api/grades/grade123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(gradeRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should require authentication for DELETE /api/grades/{id}")
    void shouldRequireAuthForDeleteGrade() throws Exception {
        mockMvc.perform(delete("/api/grades/grade123"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should verify all grade endpoints are protected")
    void shouldVerifyAllEndpointsProtected() throws Exception {
        String[] protectedPaths = {
            "/api/grades",
            "/api/grades/",
            "/api/grades/student/test@test.com",
            "/api/grades/course/123",
            "/api/grades/student/test@test.com/course/123",
            "/api/grades/faculty/faculty@test.com"
        };

        for (String path : protectedPaths) {
            mockMvc.perform(get(path))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Test
    @DisplayName("Should allow OPTIONS requests for CORS")
    void shouldAllowOptionsRequests() throws Exception {
        mockMvc.perform(options("/api/grades"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should allow actuator endpoints without authentication")
    void shouldAllowActuatorEndpoints() throws Exception {
        var result = mockMvc.perform(get("/actuator/health"))
                .andReturn();
        
        // Verify it's not forbidden (403)
        assertTrue(result.getResponse().getStatus() != 403, 
                "Actuator endpoint should not return 403 Forbidden");
    }
}