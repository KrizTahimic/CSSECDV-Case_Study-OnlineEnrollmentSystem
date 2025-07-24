package com.enrollment.course.controller;

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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for CourseController with full Spring context.
 * These tests verify the controller behavior with actual security configuration.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "jwt.secret=testsecret123456789012345678901234567890",
    "spring.data.mongodb.port=0"  // Disable MongoDB for tests
})
class CourseControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CourseService courseService;

    private Course validCourse;

    @BeforeEach
    void setUp() {
        validCourse = new Course();
        validCourse.setId("course123");
        validCourse.setCode("CS101");
        validCourse.setTitle("Introduction to Computer Science");
        validCourse.setDescription("Basic computer science concepts");
        validCourse.setCredits(3);
        validCourse.setCapacity(30);
        validCourse.setEnrolled(0);
        validCourse.setStatus("open");
        validCourse.setInstructorId("instructor123");
    }

    @Test
    @DisplayName("Should require authentication for all course endpoints")
    void shouldRequireAuthenticationForAllEndpoints() throws Exception {
        // Without authentication, all requests should return 403
        mockMvc.perform(get("/api/courses"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/courses/open"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/courses/course123"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/courses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validCourse)))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/courses/course123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validCourse)))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/courses/course123"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should allow students to view courses")
    @WithMockUser(authorities = "student")
    void shouldAllowStudentsToViewCourses() throws Exception {
        List<Course> courses = Arrays.asList(validCourse);
        when(courseService.getAllCourses()).thenReturn(courses);
        when(courseService.getOpenCourses()).thenReturn(courses);
        when(courseService.getCourseById("course123")).thenReturn(Optional.of(validCourse));

        mockMvc.perform(get("/api/courses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("CS101"));

        mockMvc.perform(get("/api/courses/open"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/courses/course123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("CS101"));
    }

    @Test
    @DisplayName("Should restrict course creation to instructors and admins")
    @WithMockUser(authorities = "student")
    void shouldRestrictCourseCreationToAuthorizedRoles() throws Exception {
        // Students should not be able to create courses
        mockMvc.perform(post("/api/courses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validCourse)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should allow instructors to create courses")
    @WithMockUser(authorities = "faculty")
    void shouldAllowInstructorsToCreateCourses() throws Exception {
        when(courseService.createCourse(any(Course.class))).thenReturn(validCourse);

        mockMvc.perform(post("/api/courses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validCourse)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("CS101"));

        verify(courseService).createCourse(any(Course.class));
    }

    @Test
    @DisplayName("Should allow admins to update any course")
    @WithMockUser(authorities = "admin")
    void shouldAllowAdminsToUpdateAnyCourse() throws Exception {
        when(courseService.updateCourse(eq("course123"), any(Course.class))).thenReturn(validCourse);

        mockMvc.perform(put("/api/courses/course123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validCourse)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should allow admins to delete courses")
    @WithMockUser(authorities = "admin")
    void shouldAllowAdminsToDeleteCourses() throws Exception {
        mockMvc.perform(delete("/api/courses/course123"))
                .andExpect(status().isOk());

        verify(courseService).deleteCourse("course123");
    }

    @Test
    @DisplayName("Should handle course not found gracefully")
    @WithMockUser(authorities = "faculty")
    void shouldHandleCourseNotFound() throws Exception {
        when(courseService.getCourseById("nonexistent")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/courses/nonexistent"))
                .andExpect(status().isNotFound());

        when(courseService.updateCourse(eq("nonexistent"), any(Course.class)))
                .thenThrow(new RuntimeException("Course not found"));

        mockMvc.perform(put("/api/courses/nonexistent")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validCourse)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should properly increment enrollment count")
    @WithMockUser(authorities = "student")
    void shouldIncrementEnrollmentCount() throws Exception {
        validCourse.setEnrolled(5);
        Course enrolledCourse = new Course();
        enrolledCourse.setId(validCourse.getId());
        enrolledCourse.setCode(validCourse.getCode());
        enrolledCourse.setEnrolled(6);

        when(courseService.incrementEnrollment("course123")).thenReturn(enrolledCourse);

        mockMvc.perform(post("/api/courses/course123/enroll"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enrolled").value(6));

        verify(courseService).incrementEnrollment("course123");
    }

    @Test
    @DisplayName("Should handle enrollment capacity exceeded")
    @WithMockUser(authorities = "student")
    void shouldHandleEnrollmentCapacityExceeded() throws Exception {
        when(courseService.incrementEnrollment("course123"))
                .thenThrow(new RuntimeException("Course is full"));

        mockMvc.perform(post("/api/courses/course123/enroll"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should properly decrement enrollment count")
    @WithMockUser(authorities = "student")
    void shouldDecrementEnrollmentCount() throws Exception {
        validCourse.setEnrolled(5);
        Course unenrolledCourse = new Course();
        unenrolledCourse.setId(validCourse.getId());
        unenrolledCourse.setCode(validCourse.getCode());
        unenrolledCourse.setEnrolled(4);

        when(courseService.decrementEnrollment("course123")).thenReturn(unenrolledCourse);

        mockMvc.perform(post("/api/courses/course123/unenroll"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enrolled").value(4));

        verify(courseService).decrementEnrollment("course123");
    }
}