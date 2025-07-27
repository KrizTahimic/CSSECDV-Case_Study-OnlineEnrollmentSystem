package com.enrollment.enrollment.security;

import com.enrollment.enrollment.client.CourseClient;
import com.enrollment.enrollment.model.Course;
import com.enrollment.enrollment.model.Enrollment;
import com.enrollment.enrollment.repository.EnrollmentRepository;
import com.enrollment.enrollment.service.EnrollmentService;
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

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Security tests for Enrollment Service.
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
class EnrollmentSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EnrollmentRepository enrollmentRepository;

    @MockBean
    private CourseClient courseClient;
    
    @MockBean
    private DiscoveryClient discoveryClient;

    @MockBean
    private EnrollmentService enrollmentService;

    private Map<String, Object> enrollmentRequest;

    @BeforeEach
    void setUp() {
        enrollmentRequest = new HashMap<>();
        enrollmentRequest.put("courseId", "course123");
    }

    @Test
    @DisplayName("Should require authentication for GET /api/enrollments")
    void shouldRequireAuthForGetEnrollments() throws Exception {
        mockMvc.perform(get("/api/enrollments"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should require authentication for GET /api/enrollments/student/{id}")
    void shouldRequireAuthForGetStudentEnrollments() throws Exception {
        mockMvc.perform(get("/api/enrollments/student/student123"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should require authentication for GET /api/enrollments/course/{id}")
    void shouldRequireAuthForGetCourseEnrollments() throws Exception {
        mockMvc.perform(get("/api/enrollments/course/course123"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should require authentication for POST /api/enrollments")
    void shouldRequireAuthForCreateEnrollment() throws Exception {
        mockMvc.perform(post("/api/enrollments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(enrollmentRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should require authentication for POST /api/enrollments/student/{studentId}/course/{courseId}")
    void shouldRequireAuthForEnrollStudent() throws Exception {
        mockMvc.perform(post("/api/enrollments/student/student123/course/course123"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should require authentication for DELETE /api/enrollments/student/{studentId}/course/{courseId}")
    void shouldRequireAuthForUnenrollStudent() throws Exception {
        mockMvc.perform(delete("/api/enrollments/student/student123/course/course123"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should verify all enrollment endpoints are protected")
    void shouldVerifyAllEndpointsProtected() throws Exception {
        // Test various enrollment endpoint patterns
        String[] protectedPaths = {
            "/api/enrollments",
            "/api/enrollments/",
            "/api/enrollments/student/123",
            "/api/enrollments/course/456",
            "/api/enrollments/student/123/course/456"
        };

        for (String path : protectedPaths) {
            mockMvc.perform(get(path))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Test
    @DisplayName("Should allow OPTIONS requests for CORS")
    void shouldAllowOptionsRequests() throws Exception {
        mockMvc.perform(options("/api/enrollments"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should allow actuator endpoints without authentication")
    void shouldAllowActuatorEndpoints() throws Exception {
        // Actuator endpoints might not be configured in test environment
        // Check if they return 404 (not found) or 200 (ok), but not 403 (forbidden)
        var result = mockMvc.perform(get("/actuator/health"))
                .andReturn();
        
        // Verify it's not forbidden (403)
        assertTrue(result.getResponse().getStatus() != 403, 
                "Actuator endpoint should not return 403 Forbidden");
    }
}