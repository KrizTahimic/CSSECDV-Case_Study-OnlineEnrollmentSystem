package com.enrollment.enrollment.controller;

import com.enrollment.enrollment.model.Course;
import com.enrollment.enrollment.model.Enrollment;
import com.enrollment.enrollment.service.EnrollmentService;
import com.enrollment.enrollment.security.JwtAuthenticationFilter;
import com.enrollment.enrollment.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
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
import static org.hamcrest.Matchers.*;

/**
 * Unit tests for EnrollmentController.
 * Tests controller logic with mocked service layer and disabled security.
 */
@WebMvcTest(value = EnrollmentController.class)
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = {
    "jwt.secret=dGVzdHNlY3JldDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDEyMzQ1Njc4OTA=",
    "jwt.expiration=86400000"
})
class EnrollmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EnrollmentService enrollmentService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private JwtService jwtService;

    private Enrollment sampleEnrollment;
    private Course sampleCourse;
    private String validJwtToken;

    @BeforeEach
    void setUp() {
        // Setup sample course
        sampleCourse = new Course();
        sampleCourse.setId("course123");
        sampleCourse.setCode("CS101");
        sampleCourse.setTitle("Introduction to Computer Science");
        sampleCourse.setDescription("Basic CS concepts");
        sampleCourse.setCredits(3);
        sampleCourse.setCapacity(30);
        sampleCourse.setEnrolled(10);
        sampleCourse.setStatus("open");

        // Setup sample enrollment
        sampleEnrollment = new Enrollment();
        sampleEnrollment.setId("enrollment123");
        sampleEnrollment.setStudentId("student@test.com");
        sampleEnrollment.setCourseId("course123");
        sampleEnrollment.setStatus("enrolled");
        sampleEnrollment.setEnrollmentDate(new Date());
        sampleEnrollment.setCourse(sampleCourse);

        // Create a valid JWT token for testing
        String secretKey = "dGVzdHNlY3JldDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDEyMzQ1Njc4OTA=";
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        Key key = Keys.hmacShaKeyFor(keyBytes);
        
        String token = Jwts.builder()
                .setSubject("student@test.com")
                .claim("role", "student")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 86400000))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
        
        validJwtToken = "Bearer " + token;
    }

    @Test
    @DisplayName("Should get student enrollments from JWT token")
    void shouldGetStudentEnrollmentsFromToken() throws Exception {
        List<Enrollment> enrollments = Arrays.asList(sampleEnrollment);
        when(enrollmentService.getStudentEnrollments(anyString())).thenReturn(enrollments);

        mockMvc.perform(get("/api/enrollments")
                .header("Authorization", validJwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value("enrollment123"))
                .andExpect(jsonPath("$[0].studentId").value("student@test.com"))
                .andExpect(jsonPath("$[0].courseId").value("course123"))
                .andExpect(jsonPath("$[0].status").value("enrolled"));

        verify(enrollmentService).getStudentEnrollments(anyString());
    }

    @Test
    @DisplayName("Should handle invalid JWT token")
    void shouldHandleInvalidJwtToken() throws Exception {
        mockMvc.perform(get("/api/enrollments")
                .header("Authorization", "Bearer invalid.jwt.token"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should get enrollments by student ID")
    void shouldGetEnrollmentsByStudentId() throws Exception {
        List<Enrollment> enrollments = Arrays.asList(sampleEnrollment);
        when(enrollmentService.getStudentEnrollments("student123")).thenReturn(enrollments);

        mockMvc.perform(get("/api/enrollments/student/student123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value("enrollment123"));

        verify(enrollmentService).getStudentEnrollments("student123");
    }

    @Test
    @DisplayName("Should get enrollments by course ID")
    void shouldGetEnrollmentsByCourseId() throws Exception {
        List<Enrollment> enrollments = Arrays.asList(sampleEnrollment);
        when(enrollmentService.getCourseEnrollments("course123")).thenReturn(enrollments);

        mockMvc.perform(get("/api/enrollments/course/course123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].courseId").value("course123"));

        verify(enrollmentService).getCourseEnrollments("course123");
    }

    @Test
    @DisplayName("Should enroll student from request")
    void shouldEnrollStudentFromRequest() throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("courseId", "course123");

        when(enrollmentService.enrollStudent(anyString(), eq("course123"))).thenReturn(sampleEnrollment);

        mockMvc.perform(post("/api/enrollments")
                .header("Authorization", validJwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("enrollment123"))
                .andExpect(jsonPath("$.courseId").value("course123"));

        verify(enrollmentService).enrollStudent(anyString(), eq("course123"));
    }

    @Test
    @DisplayName("Should handle enrollment without course ID")
    void shouldHandleEnrollmentWithoutCourseId() throws Exception {
        Map<String, Object> request = new HashMap<>();
        // Missing courseId

        mockMvc.perform(post("/api/enrollments")
                .header("Authorization", validJwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should enroll student with path variables")
    void shouldEnrollStudentWithPathVariables() throws Exception {
        when(enrollmentService.enrollStudent("student123", "course123")).thenReturn(sampleEnrollment);

        mockMvc.perform(post("/api/enrollments/student/student123/course/course123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("enrollment123"));

        verify(enrollmentService).enrollStudent("student123", "course123");
    }

    @Test
    @DisplayName("Should handle enrollment service exception")
    void shouldHandleEnrollmentServiceException() throws Exception {
        when(enrollmentService.enrollStudent("student123", "course123"))
                .thenThrow(new RuntimeException("Course is full"));

        mockMvc.perform(post("/api/enrollments/student/student123/course/course123"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should unenroll student")
    void shouldUnenrollStudent() throws Exception {
        doNothing().when(enrollmentService).unenrollStudent("student123", "course123");

        mockMvc.perform(delete("/api/enrollments/student/student123/course/course123"))
                .andExpect(status().isOk());

        verify(enrollmentService).unenrollStudent("student123", "course123");
    }

    @Test
    @DisplayName("Should handle unenrollment exception")
    void shouldHandleUnenrollmentException() throws Exception {
        doThrow(new RuntimeException("Enrollment not found"))
                .when(enrollmentService).unenrollStudent("student123", "course123");

        mockMvc.perform(delete("/api/enrollments/student/student123/course/course123"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Enrollment not found"));
    }

    @Test
    @DisplayName("Should return empty list when no enrollments found")
    void shouldReturnEmptyListWhenNoEnrollmentsFound() throws Exception {
        when(enrollmentService.getStudentEnrollments("student123")).thenReturn(Arrays.asList());

        mockMvc.perform(get("/api/enrollments/student/student123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("Should handle missing authorization header")
    void shouldHandleMissingAuthorizationHeader() throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("courseId", "course123");

        mockMvc.perform(post("/api/enrollments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}