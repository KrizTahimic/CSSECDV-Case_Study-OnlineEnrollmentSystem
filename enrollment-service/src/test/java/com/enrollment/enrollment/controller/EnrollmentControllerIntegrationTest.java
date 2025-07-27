package com.enrollment.enrollment.controller;

import com.enrollment.enrollment.client.CourseClient;
import com.enrollment.enrollment.config.TestMongoConfig;
import com.enrollment.enrollment.model.Course;
import com.enrollment.enrollment.model.Enrollment;
import com.enrollment.enrollment.repository.EnrollmentRepository;
import com.enrollment.enrollment.service.EnrollmentService;
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
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

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

/**
 * Integration tests for EnrollmentController with full Spring context.
 * Tests controller behavior with actual security configuration and role-based access.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestMongoConfig.class)
@TestPropertySource(properties = {
    "jwt.secret=dGVzdHNlY3JldDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDEyMzQ1Njc4OTA=",
    "jwt.expiration=86400000",
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration,org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration",
    "eureka.client.enabled=false",
    "spring.cloud.discovery.enabled=false"
})
class EnrollmentControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EnrollmentService enrollmentService;
    
    @MockBean
    private EnrollmentRepository enrollmentRepository;
    
    @MockBean
    private CourseClient courseClient;
    
    @MockBean
    private DiscoveryClient discoveryClient;

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
        sampleCourse.setStatus("open");
        sampleCourse.setCapacity(30);
        sampleCourse.setEnrolled(10);

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
    @DisplayName("Should require authentication for all enrollment endpoints")
    void shouldRequireAuthenticationForAllEndpoints() throws Exception {
        // Without authentication, all requests should return 401
        mockMvc.perform(get("/api/enrollments"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/enrollments/student/student123"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/enrollments/course/course123"))
                .andExpect(status().isUnauthorized());

        Map<String, Object> request = new HashMap<>();
        request.put("courseId", "course123");

        mockMvc.perform(post("/api/enrollments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/enrollments/student/student123/course/course123"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(delete("/api/enrollments/student/student123/course/course123"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should allow students to view their own enrollments")
    @WithMockUser(username = "student@test.com", roles = "STUDENT")
    void shouldAllowStudentsToViewOwnEnrollments() throws Exception {
        List<Enrollment> enrollments = Arrays.asList(sampleEnrollment);
        when(enrollmentService.getStudentEnrollments("student@test.com")).thenReturn(enrollments);

        mockMvc.perform(get("/api/enrollments/student/student@test.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("enrollment123"));

        verify(enrollmentService).getStudentEnrollments("student@test.com");
    }

    @Test
    @DisplayName("Should prevent students from viewing other students' enrollments")
    @WithMockUser(username = "student1@test.com", roles = "STUDENT")
    void shouldPreventStudentsFromViewingOthersEnrollments() throws Exception {
        mockMvc.perform(get("/api/enrollments/student/student2@test.com"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should allow faculty to view any student's enrollments")
    @WithMockUser(roles = "FACULTY")
    void shouldAllowFacultyToViewAnyStudentEnrollments() throws Exception {
        List<Enrollment> enrollments = Arrays.asList(sampleEnrollment);
        when(enrollmentService.getStudentEnrollments("student123")).thenReturn(enrollments);

        mockMvc.perform(get("/api/enrollments/student/student123"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should allow faculty to view course enrollments")
    @WithMockUser(roles = "FACULTY")
    void shouldAllowFacultyToViewCourseEnrollments() throws Exception {
        List<Enrollment> enrollments = Arrays.asList(sampleEnrollment);
        when(enrollmentService.getCourseEnrollments("course123")).thenReturn(enrollments);

        mockMvc.perform(get("/api/enrollments/course/course123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].courseId").value("course123"));
    }

    @Test
    @DisplayName("Should prevent students from viewing course enrollments")
    @WithMockUser(roles = "STUDENT")
    void shouldPreventStudentsFromViewingCourseEnrollments() throws Exception {
        mockMvc.perform(get("/api/enrollments/course/course123"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should allow students to enroll themselves")
    @WithMockUser(username = "student@test.com", roles = "STUDENT")
    void shouldAllowStudentsToEnrollThemselves() throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("courseId", "course123");

        when(enrollmentService.enrollStudent(anyString(), eq("course123"))).thenReturn(sampleEnrollment);

        mockMvc.perform(post("/api/enrollments")
                .header("Authorization", validJwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should prevent faculty from using student enrollment endpoint")
    @WithMockUser(roles = "FACULTY")
    void shouldPreventFacultyFromStudentEnrollment() throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("courseId", "course123");

        // Create faculty JWT token
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

        mockMvc.perform(post("/api/enrollments")
                .header("Authorization", "Bearer " + facultyToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should allow admin to manually enroll students")
    @WithMockUser(roles = "ADMIN")
    void shouldAllowAdminToManuallyEnrollStudents() throws Exception {
        when(enrollmentService.enrollStudent("student123", "course123")).thenReturn(sampleEnrollment);

        mockMvc.perform(post("/api/enrollments/student/student123/course/course123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("enrollment123"));

        verify(enrollmentService).enrollStudent("student123", "course123");
    }

    @Test
    @DisplayName("Should prevent students from manually enrolling others")
    @WithMockUser(roles = "STUDENT")
    void shouldPreventStudentsFromManuallyEnrollingOthers() throws Exception {
        mockMvc.perform(post("/api/enrollments/student/student123/course/course123"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should allow students to drop their own courses")
    @WithMockUser(username = "student@test.com", roles = "STUDENT")
    void shouldAllowStudentsToDropOwnCourses() throws Exception {
        doNothing().when(enrollmentService).unenrollStudent("student@test.com", "course123");

        mockMvc.perform(delete("/api/enrollments/student/student@test.com/course/course123"))
                .andExpect(status().isOk());

        verify(enrollmentService).unenrollStudent("student@test.com", "course123");
    }

    @Test
    @DisplayName("Should prevent students from dropping others' courses")
    @WithMockUser(username = "student1@test.com", roles = "STUDENT")
    void shouldPreventStudentsFromDroppingOthersCourses() throws Exception {
        mockMvc.perform(delete("/api/enrollments/student/student2@test.com/course/course123"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should allow admin to drop any enrollment")
    @WithMockUser(roles = "ADMIN")
    void shouldAllowAdminToDropAnyEnrollment() throws Exception {
        doNothing().when(enrollmentService).unenrollStudent("student123", "course123");

        mockMvc.perform(delete("/api/enrollments/student/student123/course/course123"))
                .andExpect(status().isOk());

        verify(enrollmentService).unenrollStudent("student123", "course123");
    }

    @Test
    @DisplayName("Should handle enrollment service exceptions gracefully")
    @WithMockUser(roles = "ADMIN")
    void shouldHandleServiceExceptionsGracefully() throws Exception {
        when(enrollmentService.enrollStudent("student123", "course123"))
                .thenThrow(new RuntimeException("Course is full"));

        mockMvc.perform(post("/api/enrollments/student/student123/course/course123"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should validate authorization with case-insensitive roles")
    @WithMockUser(roles = "STUDENT")
    void shouldValidateAuthorizationCaseInsensitive() throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("courseId", "course123");

        when(enrollmentService.enrollStudent(anyString(), eq("course123"))).thenReturn(sampleEnrollment);

        // Create Student JWT token (with capital S)
        String secretKey = "dGVzdHNlY3JldDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDEyMzQ1Njc4OTA=";
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        Key key = Keys.hmacShaKeyFor(keyBytes);
        
        String studentToken = Jwts.builder()
                .setSubject("student@test.com")
                .claim("role", "Student")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 86400000))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        mockMvc.perform(post("/api/enrollments")
                .header("Authorization", "Bearer " + studentToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }
}