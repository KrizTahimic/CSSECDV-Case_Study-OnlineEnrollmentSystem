package com.enrollment.course.controller;

import com.enrollment.course.model.Course;
import com.enrollment.course.model.Schedule;
import com.enrollment.course.service.CourseService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;

/**
 * Tests for CourseController including security and validation checks.
 * Verifies implementation of:
 * - 2.1.1: Authentication requirements for protected resources
 * - 2.2.1: Access control for course management
 * - 2.3.1: Input validation
 */
@WebMvcTest(CourseController.class)
class CourseControllerTest {

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
        // GET endpoints should require authentication
        mockMvc.perform(get("/api/courses"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/courses/open"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/courses/course123"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/courses/code/CS101"))
                .andExpect(status().isUnauthorized());

        // POST endpoints should require authentication
        mockMvc.perform(post("/api/courses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validCourse)))
                .andExpect(status().isUnauthorized());

        // PUT endpoints should require authentication
        mockMvc.perform(put("/api/courses/course123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validCourse)))
                .andExpect(status().isUnauthorized());

        // DELETE endpoints should require authentication
        mockMvc.perform(delete("/api/courses/course123"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should allow students to view courses")
    @WithMockUser(roles = "STUDENT")
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
    @WithMockUser(roles = "STUDENT")
    void shouldRestrictCourseCreationToAuthorizedRoles() throws Exception {
        // Students should not be able to create courses
        mockMvc.perform(post("/api/courses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validCourse)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should allow instructors to create courses")
    @WithMockUser(roles = "INSTRUCTOR")
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
    @DisplayName("Should validate required fields on course creation")
    @WithMockUser(roles = "INSTRUCTOR")
    void shouldValidateRequiredFieldsOnCreation() throws Exception {
        Course invalidCourse = new Course();
        // Missing required fields

        mockMvc.perform(post("/api/courses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidCourse)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should validate course capacity is positive")
    @WithMockUser(roles = "INSTRUCTOR")
    void shouldValidateCourseCapacity() throws Exception {
        validCourse.setCapacity(-1);

        mockMvc.perform(post("/api/courses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validCourse)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should validate course credits range")
    @WithMockUser(roles = "INSTRUCTOR")
    void shouldValidateCourseCredits() throws Exception {
        // Credits should be between 1-6
        validCourse.setCredits(0);

        mockMvc.perform(post("/api/courses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validCourse)))
                .andExpect(status().isBadRequest());

        validCourse.setCredits(10);

        mockMvc.perform(post("/api/courses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validCourse)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should restrict course updates to course instructor or admin")
    @WithMockUser(username = "instructor123", roles = "INSTRUCTOR")
    void shouldRestrictCourseUpdatesToAuthorized() throws Exception {
        when(courseService.getCourseById("course123")).thenReturn(Optional.of(validCourse));
        when(courseService.updateCourse(eq("course123"), any(Course.class))).thenReturn(validCourse);

        // Instructor who owns the course should be able to update
        mockMvc.perform(put("/api/courses/course123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validCourse)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should prevent other instructors from updating courses they don't own")
    @WithMockUser(username = "otherinstructor", roles = "INSTRUCTOR")
    void shouldPreventUnauthorizedCourseUpdates() throws Exception {
        when(courseService.getCourseById("course123")).thenReturn(Optional.of(validCourse));

        mockMvc.perform(put("/api/courses/course123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validCourse)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should allow admins to update any course")
    @WithMockUser(roles = "ADMIN")
    void shouldAllowAdminsToUpdateAnyCourse() throws Exception {
        when(courseService.updateCourse(eq("course123"), any(Course.class))).thenReturn(validCourse);

        mockMvc.perform(put("/api/courses/course123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validCourse)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should handle course not found gracefully")
    @WithMockUser(roles = "INSTRUCTOR")
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
    @WithMockUser(roles = "STUDENT")
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
    @WithMockUser(roles = "STUDENT")
    void shouldHandleEnrollmentCapacityExceeded() throws Exception {
        when(courseService.incrementEnrollment("course123"))
                .thenThrow(new RuntimeException("Course is full"));

        mockMvc.perform(post("/api/courses/course123/enroll"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should properly decrement enrollment count")
    @WithMockUser(roles = "STUDENT")
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

    @Test
    @DisplayName("Should validate course code format")
    @WithMockUser(roles = "INSTRUCTOR")
    void shouldValidateCourseCodeFormat() throws Exception {
        // Course code should be alphanumeric and 2-10 characters
        validCourse.setCode("!@#$%");

        mockMvc.perform(post("/api/courses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validCourse)))
                .andExpect(status().isBadRequest());

        validCourse.setCode("A"); // Too short

        mockMvc.perform(post("/api/courses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validCourse)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should sanitize input to prevent XSS")
    @WithMockUser(roles = "INSTRUCTOR")
    void shouldSanitizeInputToPreventXSS() throws Exception {
        validCourse.setTitle("<script>alert('XSS')</script>");
        validCourse.setDescription("<img src=x onerror=alert('XSS')>");

        mockMvc.perform(post("/api/courses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validCourse)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid input data"));
    }
}