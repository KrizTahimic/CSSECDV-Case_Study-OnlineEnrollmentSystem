package com.enrollment.course.controller;

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
import com.enrollment.course.config.TestSecurityConfig;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for CourseController focusing on controller logic.
 * Security is tested separately in CourseControllerIntegrationTest.
 */
@WebMvcTest(CourseController.class)
@Import(TestSecurityConfig.class)
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
    @DisplayName("Should get all courses")
    void shouldGetAllCourses() throws Exception {
        List<Course> courses = Arrays.asList(validCourse);
        when(courseService.getAllCourses()).thenReturn(courses);

        mockMvc.perform(get("/api/courses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("CS101"));
    }

    @Test
    @DisplayName("Should get open courses")
    void shouldGetOpenCourses() throws Exception {
        List<Course> courses = Arrays.asList(validCourse);
        when(courseService.getOpenCourses()).thenReturn(courses);

        mockMvc.perform(get("/api/courses/open"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("CS101"));
    }

    @Test
    @DisplayName("Should get course by ID")
    void shouldGetCourseById() throws Exception {
        when(courseService.getCourseById("course123")).thenReturn(Optional.of(validCourse));

        mockMvc.perform(get("/api/courses/course123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("CS101"));
    }

    @Test
    @DisplayName("Should return 404 when course not found")
    void shouldReturn404WhenCourseNotFound() throws Exception {
        when(courseService.getCourseById("nonexistent")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/courses/nonexistent"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should create course with valid data")
    void shouldCreateCourseWithValidData() throws Exception {
        when(courseService.createCourse(any(Course.class))).thenReturn(validCourse);

        mockMvc.perform(post("/api/courses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validCourse)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("CS101"));

        verify(courseService).createCourse(any(Course.class));
    }

    @Test
    @DisplayName("Should update course")
    void shouldUpdateCourse() throws Exception {
        when(courseService.updateCourse(eq("course123"), any(Course.class))).thenReturn(validCourse);

        mockMvc.perform(put("/api/courses/course123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validCourse)))
                .andExpect(status().isOk());

        verify(courseService).updateCourse(eq("course123"), any(Course.class));
    }

    @Test
    @DisplayName("Should return 404 when updating non-existent course")
    void shouldReturn404WhenUpdatingNonExistentCourse() throws Exception {
        when(courseService.updateCourse(eq("nonexistent"), any(Course.class)))
                .thenThrow(new RuntimeException("Course not found"));

        mockMvc.perform(put("/api/courses/nonexistent")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validCourse)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should delete course")
    void shouldDeleteCourse() throws Exception {
        mockMvc.perform(delete("/api/courses/course123"))
                .andExpect(status().isOk());

        verify(courseService).deleteCourse("course123");
    }

    @Test
    @DisplayName("Should increment enrollment")
    void shouldIncrementEnrollment() throws Exception {
        Course enrolledCourse = new Course();
        enrolledCourse.setId(validCourse.getId());
        enrolledCourse.setCode(validCourse.getCode());
        enrolledCourse.setEnrolled(1);

        when(courseService.incrementEnrollment("course123")).thenReturn(enrolledCourse);

        mockMvc.perform(post("/api/courses/course123/enroll"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enrolled").value(1));

        verify(courseService).incrementEnrollment("course123");
    }

    @Test
    @DisplayName("Should handle enrollment capacity exceeded")
    void shouldHandleEnrollmentCapacityExceeded() throws Exception {
        when(courseService.incrementEnrollment("course123"))
                .thenThrow(new RuntimeException("Course is full"));

        mockMvc.perform(post("/api/courses/course123/enroll"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should decrement enrollment")
    void shouldDecrementEnrollment() throws Exception {
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
    @DisplayName("Should handle decrement below zero")
    void shouldHandleDecrementBelowZero() throws Exception {
        when(courseService.decrementEnrollment("course123"))
                .thenThrow(new RuntimeException("Cannot decrement below zero"));

        mockMvc.perform(post("/api/courses/course123/unenroll"))
                .andExpect(status().isBadRequest());
    }
}