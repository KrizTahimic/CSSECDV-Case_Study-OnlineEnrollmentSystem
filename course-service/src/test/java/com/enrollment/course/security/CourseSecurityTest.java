package com.enrollment.course.security;

import com.enrollment.course.config.SecurityConfig;
import com.enrollment.course.controller.CourseController;
import com.enrollment.course.model.Course;
import com.enrollment.course.service.CourseService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Security tests for Course Service.
 * Tests requirement 2.1.1: Require authentication for all pages and resources,
 * except those specifically intended to be public.
 * 
 * CRITICAL: Currently, the SecurityConfig allows all course endpoints without authentication!
 * This test demonstrates the security vulnerability.
 */
@WebMvcTest(CourseController.class)
@Import(SecurityConfig.class)
class CourseSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CourseService courseService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

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
    @DisplayName("SECURITY VULNERABILITY: Course endpoints are publicly accessible without authentication")
    void demonstrateSecurityVulnerability() throws Exception {
        // This test PASSES, which means there's a security vulnerability!
        // All these endpoints should require authentication but don't.
        
        when(courseService.getAllCourses()).thenReturn(Arrays.asList(testCourse));
        when(courseService.getOpenCourses()).thenReturn(Arrays.asList(testCourse));
        when(courseService.getCourseById("course123")).thenReturn(Optional.of(testCourse));
        when(courseService.getCourseByCode("CS101")).thenReturn(Optional.of(testCourse));
        when(courseService.createCourse(any(Course.class))).thenReturn(testCourse);
        when(courseService.updateCourse(eq("course123"), any(Course.class))).thenReturn(testCourse);
        
        // GET endpoints - should require authentication but don't
        mockMvc.perform(get("/api/courses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("CS101"));
        
        mockMvc.perform(get("/api/courses/open"))
                .andExpect(status().isOk());
        
        mockMvc.perform(get("/api/courses/course123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("CS101"));
        
        mockMvc.perform(get("/api/courses/code/CS101"))
                .andExpect(status().isOk());
        
        // POST endpoint - should require authentication AND authorization but doesn't
        mockMvc.perform(post("/api/courses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testCourse)))
                .andExpect(status().isOk());
        
        // PUT endpoint - should require authentication AND authorization but doesn't
        mockMvc.perform(put("/api/courses/course123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testCourse)))
                .andExpect(status().isOk());
        
        // DELETE endpoint - should require authentication AND authorization but doesn't
        mockMvc.perform(delete("/api/courses/course123"))
                .andExpect(status().isOk());
        
        // Enrollment endpoints - also publicly accessible!
        when(courseService.incrementEnrollment("course123")).thenReturn(testCourse);
        when(courseService.decrementEnrollment("course123")).thenReturn(testCourse);
        
        mockMvc.perform(post("/api/courses/course123/enroll"))
                .andExpect(status().isOk());
        
        mockMvc.perform(post("/api/courses/course123/unenroll"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Demonstrates that anyone can create courses without authentication")
    void anyoneCanCreateCourses() throws Exception {
        Course maliciousCourse = new Course();
        maliciousCourse.setCode("HACK101");
        maliciousCourse.setTitle("How to Hack");
        maliciousCourse.setDescription("Malicious course created without auth");
        maliciousCourse.setCredits(3);
        maliciousCourse.setCapacity(100);
        
        when(courseService.createCourse(any(Course.class))).thenReturn(maliciousCourse);
        
        // This should fail but doesn't - anyone can create courses!
        mockMvc.perform(post("/api/courses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(maliciousCourse)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("HACK101"));
        
        verify(courseService).createCourse(any(Course.class));
    }

    @Test
    @DisplayName("Demonstrates that anyone can delete courses without authentication")
    void anyoneCanDeleteCourses() throws Exception {
        // This should fail but doesn't - anyone can delete any course!
        mockMvc.perform(delete("/api/courses/course123"))
                .andExpect(status().isOk());
        
        verify(courseService).deleteCourse("course123");
    }

    @Test
    @DisplayName("Shows OPTIONS requests are properly allowed for CORS")
    void optionsRequestsAllowedForCORS() throws Exception {
        // OPTIONS should be allowed for CORS preflight
        mockMvc.perform(options("/api/courses"))
                .andExpect(status().isOk());
    }
}