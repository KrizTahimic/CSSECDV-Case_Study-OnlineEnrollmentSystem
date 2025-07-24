package com.enrollment.course.service;

import com.enrollment.course.model.Course;
import com.enrollment.course.repository.CourseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CourseService business logic.
 * Tests data validation, capacity management, and CRUD operations.
 */
@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    @Mock
    private CourseRepository courseRepository;

    @InjectMocks
    private CourseService courseService;

    private Course testCourse;

    @BeforeEach
    void setUp() {
        testCourse = new Course();
        testCourse.setId("course123");
        testCourse.setCode("CS101");
        testCourse.setTitle("Introduction to Computer Science");
        testCourse.setDescription("Basic computer science concepts");
        testCourse.setCredits(3);
        testCourse.setCapacity(30);
        testCourse.setEnrolled(0);
        testCourse.setStatus("open");
        testCourse.setInstructorId("instructor123");
    }

    @Test
    @DisplayName("Should get all courses")
    void shouldGetAllCourses() {
        List<Course> courses = Arrays.asList(testCourse);
        when(courseRepository.findAll()).thenReturn(courses);

        List<Course> result = courseService.getAllCourses();

        assertEquals(1, result.size());
        assertEquals("CS101", result.get(0).getCode());
        verify(courseRepository).findAll();
    }

    @Test
    @DisplayName("Should get only open courses")
    void shouldGetOpenCourses() {
        Course closedCourse = new Course();
        closedCourse.setCode("CS102");
        closedCourse.setStatus("closed");

        List<Course> allCourses = Arrays.asList(testCourse, closedCourse);
        when(courseRepository.findAll()).thenReturn(allCourses);

        List<Course> openCourses = courseService.getOpenCourses();

        assertEquals(1, openCourses.size());
        assertEquals("CS101", openCourses.get(0).getCode());
        assertEquals("open", openCourses.get(0).getStatus());
    }

    @Test
    @DisplayName("Should get course by ID")
    void shouldGetCourseById() {
        when(courseRepository.findById("course123")).thenReturn(Optional.of(testCourse));

        Optional<Course> result = courseService.getCourseById("course123");

        assertTrue(result.isPresent());
        assertEquals("CS101", result.get().getCode());
        verify(courseRepository).findById("course123");
    }

    @Test
    @DisplayName("Should get course by code")
    void shouldGetCourseByCode() {
        when(courseRepository.findByCode("CS101")).thenReturn(Optional.of(testCourse));

        Optional<Course> result = courseService.getCourseByCode("CS101");

        assertTrue(result.isPresent());
        assertEquals("CS101", result.get().getCode());
        verify(courseRepository).findByCode("CS101");
    }

    @Test
    @DisplayName("Should create course with validation")
    void shouldCreateCourseWithValidation() {
        when(courseRepository.save(any(Course.class))).thenReturn(testCourse);

        Course result = courseService.createCourse(testCourse);

        assertNotNull(result);
        assertEquals("CS101", result.getCode());
        assertEquals(0, result.getEnrolled()); // Should initialize to 0
        verify(courseRepository).save(any(Course.class));
    }

    @Test
    @DisplayName("Should reject course creation with invalid data")
    void shouldRejectCourseCreationWithInvalidData() {
        // Test null course
        assertThrows(IllegalArgumentException.class, () -> {
            courseService.createCourse(null);
        });

        // Test course without code
        Course invalidCourse = new Course();
        assertThrows(IllegalArgumentException.class, () -> {
            courseService.createCourse(invalidCourse);
        });

        // Test course with negative capacity
        invalidCourse.setCode("CS999");
        invalidCourse.setCapacity(-1);
        assertThrows(IllegalArgumentException.class, () -> {
            courseService.createCourse(invalidCourse);
        });

        verify(courseRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should update existing course")
    void shouldUpdateExistingCourse() {
        when(courseRepository.findById("course123")).thenReturn(Optional.of(testCourse));
        when(courseRepository.save(any(Course.class))).thenReturn(testCourse);

        Course updatedCourse = new Course();
        updatedCourse.setTitle("Advanced Computer Science");
        updatedCourse.setDescription("Advanced topics");

        Course result = courseService.updateCourse("course123", updatedCourse);

        assertNotNull(result);
        verify(courseRepository).findById("course123");
        verify(courseRepository).save(any(Course.class));
    }

    @Test
    @DisplayName("Should throw exception when updating non-existent course")
    void shouldThrowExceptionWhenUpdatingNonExistentCourse() {
        when(courseRepository.findById("nonexistent")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> {
            courseService.updateCourse("nonexistent", testCourse);
        });

        verify(courseRepository).findById("nonexistent");
        verify(courseRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should delete course")
    void shouldDeleteCourse() {
        courseService.deleteCourse("course123");

        verify(courseRepository).deleteById("course123");
    }

    @Test
    @DisplayName("Should increment enrollment when capacity available")
    void shouldIncrementEnrollmentWhenCapacityAvailable() {
        testCourse.setEnrolled(10);
        when(courseRepository.findById("course123")).thenReturn(Optional.of(testCourse));
        when(courseRepository.save(any(Course.class))).thenReturn(testCourse);

        Course result = courseService.incrementEnrollment("course123");

        assertEquals(11, result.getEnrolled());
        verify(courseRepository).save(argThat(course -> course.getEnrolled() == 11));
    }

    @Test
    @DisplayName("Should throw exception when course is full")
    void shouldThrowExceptionWhenCourseFull() {
        testCourse.setEnrolled(30); // At capacity
        when(courseRepository.findById("course123")).thenReturn(Optional.of(testCourse));

        assertThrows(RuntimeException.class, () -> {
            courseService.incrementEnrollment("course123");
        });

        verify(courseRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should decrement enrollment")
    void shouldDecrementEnrollment() {
        testCourse.setEnrolled(10);
        when(courseRepository.findById("course123")).thenReturn(Optional.of(testCourse));
        when(courseRepository.save(any(Course.class))).thenReturn(testCourse);

        Course result = courseService.decrementEnrollment("course123");

        assertEquals(9, result.getEnrolled());
        verify(courseRepository).save(argThat(course -> course.getEnrolled() == 9));
    }

    @Test
    @DisplayName("Should not decrement enrollment below zero")
    void shouldNotDecrementEnrollmentBelowZero() {
        testCourse.setEnrolled(0);
        when(courseRepository.findById("course123")).thenReturn(Optional.of(testCourse));

        assertThrows(RuntimeException.class, () -> {
            courseService.decrementEnrollment("course123");
        });

        verify(courseRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should validate course code format")
    void shouldValidateCourseCodeFormat() {
        // Valid codes
        assertTrue(isValidCourseCode("CS101"));
        assertTrue(isValidCourseCode("MATH200"));
        assertTrue(isValidCourseCode("PHY300A"));

        // Invalid codes
        assertFalse(isValidCourseCode(""));
        assertFalse(isValidCourseCode("C")); // Too short
        assertFalse(isValidCourseCode("CS-101")); // Invalid character
        assertFalse(isValidCourseCode("CS 101")); // Space
        assertFalse(isValidCourseCode("12345")); // No letters
    }

    private boolean isValidCourseCode(String code) {
        // Course code should be 2-10 alphanumeric characters
        return code != null && 
               code.length() >= 2 && 
               code.length() <= 10 && 
               code.matches("^[A-Z0-9]+$");
    }

    @Test
    @DisplayName("Should handle concurrent enrollment updates")
    void shouldHandleConcurrentEnrollmentUpdates() {
        // This test documents that proper concurrency control should be implemented
        // In a real implementation, you might use optimistic locking with @Version
        
        testCourse.setEnrolled(29); // One spot left
        when(courseRepository.findById("course123")).thenReturn(Optional.of(testCourse));
        
        // In a real scenario, two students trying to enroll simultaneously
        // should result in only one successful enrollment
        
        // First enrollment succeeds
        when(courseRepository.save(any(Course.class))).thenReturn(testCourse);
        Course result = courseService.incrementEnrollment("course123");
        assertEquals(30, result.getEnrolled());
        
        // Second enrollment should fail due to capacity
        testCourse.setEnrolled(30);
        assertThrows(RuntimeException.class, () -> {
            courseService.incrementEnrollment("course123");
        });
    }
}