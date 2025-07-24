package com.enrollment.course.security;

import com.enrollment.course.model.Course;
import com.enrollment.course.service.CourseService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;


import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Security tests for Course Service.
 * Tests requirement 2.1.1: Require authentication for all pages and resources,
 * except those specifically intended to be public.
 * 
 * This test verifies that authentication is properly required for all course endpoints.
 */
@SpringBootTest
@AutoConfigureMockMvc
class CourseSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CourseService courseService;

    private Course testCourse;

    @BeforeEach
    void setUp() {
        testCourse = new Course();
        testCourse.setId("course123");
        testCourse.setCode("CS101");
        testCourse.setTitle("Introduction to Computer Science");
        testCourse.setDescription("Basic CS concepts");
        testCourse.setCredits(3);
        testCourse.setCapacity(30);
        testCourse.setEnrolled(0);
        testCourse.setStatus("open");
    }

    @Test
    @DisplayName("Course endpoints require authentication")
    void endpointsRequireAuthentication() throws Exception {
        // All course endpoints should return 403 Forbidden without authentication
        
        // GET endpoints - require authentication
        mockMvc.perform(get("/api/courses"))
                .andExpect(status().isForbidden());
        
        mockMvc.perform(get("/api/courses/open"))
                .andExpect(status().isForbidden());
        
        mockMvc.perform(get("/api/courses/course123"))
                .andExpect(status().isForbidden());
        
        mockMvc.perform(get("/api/courses/code/CS101"))
                .andExpect(status().isForbidden());
        
        // POST endpoint - requires authentication
        mockMvc.perform(post("/api/courses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testCourse)))
                .andExpect(status().isForbidden());
        
        // PUT endpoint - requires authentication
        mockMvc.perform(put("/api/courses/course123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testCourse)))
                .andExpect(status().isForbidden());
        
        // DELETE endpoint - requires authentication
        mockMvc.perform(delete("/api/courses/course123"))
                .andExpect(status().isForbidden());
        
        // Enrollment endpoints - require authentication
        mockMvc.perform(post("/api/courses/course123/enroll"))
                .andExpect(status().isForbidden());
        
        mockMvc.perform(post("/api/courses/course123/unenroll"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Unauthenticated users cannot create courses")
    void unauthenticatedCannotCreateCourses() throws Exception {
        Course maliciousCourse = new Course();
        maliciousCourse.setCode("HACK101");
        maliciousCourse.setTitle("How to Hack");
        maliciousCourse.setDescription("Malicious course created without auth");
        maliciousCourse.setCredits(3);
        maliciousCourse.setCapacity(100);
        
        // Without authentication, should get 403 Forbidden
        mockMvc.perform(post("/api/courses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(maliciousCourse)))
                .andExpect(status().isForbidden());
        
        // Service should never be called
        verify(courseService, never()).createCourse(any(Course.class));
    }

    @Test
    @DisplayName("Unauthenticated users cannot delete courses")
    void unauthenticatedCannotDeleteCourses() throws Exception {
        // Without authentication, should get 403 Forbidden
        mockMvc.perform(delete("/api/courses/course123"))
                .andExpect(status().isForbidden());
        
        // Service should never be called
        verify(courseService, never()).deleteCourse("course123");
    }

    @Test
    @DisplayName("Shows OPTIONS requests are properly allowed for CORS")
    void optionsRequestsAllowedForCORS() throws Exception {
        // OPTIONS should be allowed for CORS preflight
        mockMvc.perform(options("/api/courses"))
                .andExpect(status().isOk());
    }
}