package com.enrollment.enrollment.service;

import com.enrollment.enrollment.client.CourseClient;
import com.enrollment.enrollment.model.Course;
import com.enrollment.enrollment.model.Enrollment;
import com.enrollment.enrollment.repository.EnrollmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for EnrollmentService.
 * Tests business logic with mocked repository and external clients.
 */
@ExtendWith(MockitoExtension.class)
class EnrollmentServiceTest {

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private CourseClient courseClient;

    @InjectMocks
    private EnrollmentService enrollmentService;

    private Enrollment sampleEnrollment;
    private Course sampleCourse;

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
    }

    @Test
    @DisplayName("Should get student enrollments with course details")
    void shouldGetStudentEnrollmentsWithCourseDetails() {
        // Arrange
        List<Enrollment> enrollments = Arrays.asList(sampleEnrollment);
        when(enrollmentRepository.findByStudentIdAndStatus("student123", "enrolled")).thenReturn(enrollments);
        when(courseClient.getCourse("course123")).thenReturn(sampleCourse);

        // Act
        List<Enrollment> result = enrollmentService.getStudentEnrollments("student123");

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("enrollment123", result.get(0).getId());
        assertNotNull(result.get(0).getCourse());
        assertEquals("CS101", result.get(0).getCourse().getCode());

        verify(enrollmentRepository).findByStudentIdAndStatus("student123", "enrolled");
        verify(courseClient).getCourse("course123");
    }

    @Test
    @DisplayName("Should handle course service unavailability gracefully")
    void shouldHandleCourseServiceUnavailability() {
        // Arrange
        List<Enrollment> enrollments = Arrays.asList(sampleEnrollment);
        when(enrollmentRepository.findByStudentIdAndStatus("student123", "enrolled")).thenReturn(enrollments);
        when(courseClient.getCourse("course123"))
                .thenThrow(new RuntimeException("Connection refused"));

        // Act & Assert
        assertThrows(RuntimeException.class, 
            () -> enrollmentService.getStudentEnrollments("student123"),
            "Course service is currently unavailable"
        );
    }

    @Test
    @DisplayName("Should get course enrollments")
    void shouldGetCourseEnrollments() {
        // Arrange
        List<Enrollment> enrollments = Arrays.asList(sampleEnrollment);
        when(enrollmentRepository.findByCourseId("course123")).thenReturn(enrollments);
        when(courseClient.getCourse("course123")).thenReturn(sampleCourse);

        // Act
        List<Enrollment> result = enrollmentService.getCourseEnrollments("course123");

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("course123", result.get(0).getCourseId());

        verify(enrollmentRepository).findByCourseId("course123");
    }

    @Test
    @DisplayName("Should enroll student in course successfully")
    void shouldEnrollStudentSuccessfully() {
        // Arrange
        when(enrollmentRepository.findByStudentIdAndCourseId("student123", "course123"))
                .thenReturn(Optional.empty());
        when(courseClient.getCourse("course123")).thenReturn(sampleCourse);
        when(enrollmentRepository.save(any(Enrollment.class))).thenReturn(sampleEnrollment);
        when(courseClient.incrementEnrollment("course123")).thenReturn(sampleCourse);

        // Act
        Enrollment result = enrollmentService.enrollStudent("student123", "course123");

        // Assert
        assertNotNull(result);
        assertEquals("enrollment123", result.getId());
        assertEquals("student@test.com", result.getStudentId());  // The mock returns sampleEnrollment which has this studentId
        assertEquals("course123", result.getCourseId());

        verify(courseClient).getCourse("course123");
        verify(courseClient).incrementEnrollment("course123");
        verify(enrollmentRepository).save(any(Enrollment.class));
    }

    @Test
    @DisplayName("Should reactivate dropped enrollment")
    void shouldReactivateDroppedEnrollment() {
        // Arrange
        Enrollment droppedEnrollment = new Enrollment();
        droppedEnrollment.setId("enrollment123");
        droppedEnrollment.setStudentId("student123");
        droppedEnrollment.setCourseId("course123");
        droppedEnrollment.setStatus("dropped");

        when(enrollmentRepository.findByStudentIdAndCourseId("student123", "course123"))
                .thenReturn(Optional.of(droppedEnrollment));
        when(courseClient.incrementEnrollment("course123")).thenReturn(sampleCourse);
        when(enrollmentRepository.save(any(Enrollment.class))).thenReturn(sampleEnrollment);

        // Act
        Enrollment result = enrollmentService.enrollStudent("student123", "course123");

        // Assert
        assertNotNull(result);
        assertEquals("enrolled", sampleEnrollment.getStatus());
        verify(courseClient).incrementEnrollment("course123");
        verify(enrollmentRepository).save(any(Enrollment.class));
    }

    @Test
    @DisplayName("Should throw exception when student already enrolled")
    void shouldThrowExceptionWhenAlreadyEnrolled() {
        // Arrange
        when(enrollmentRepository.findByStudentIdAndCourseId("student123", "course123"))
                .thenReturn(Optional.of(sampleEnrollment));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> enrollmentService.enrollStudent("student123", "course123"));
        
        assertEquals("Student is already enrolled in this course", exception.getMessage());
        verify(courseClient, never()).incrementEnrollment(anyString());
    }

    @Test
    @DisplayName("Should throw exception when course not found")
    void shouldThrowExceptionWhenCourseNotFound() {
        // Arrange
        when(enrollmentRepository.findByStudentIdAndCourseId("student123", "course123"))
                .thenReturn(Optional.empty());
        when(courseClient.getCourse("course123")).thenReturn(null);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> enrollmentService.enrollStudent("student123", "course123"));
        
        assertEquals("Course not found", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw exception when course is closed")
    void shouldThrowExceptionWhenCourseClosed() {
        // Arrange
        Course closedCourse = new Course();
        closedCourse.setId("course123");
        closedCourse.setStatus("closed");

        when(enrollmentRepository.findByStudentIdAndCourseId("student123", "course123"))
                .thenReturn(Optional.empty());
        when(courseClient.getCourse("course123")).thenReturn(closedCourse);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> enrollmentService.enrollStudent("student123", "course123"));
        
        assertEquals("Course is not open for enrollment", exception.getMessage());
    }

    @Test
    @DisplayName("Should unenroll student successfully")
    void shouldUnenrollStudentSuccessfully() {
        // Arrange
        when(enrollmentRepository.findByStudentIdAndCourseId("student123", "course123"))
                .thenReturn(Optional.of(sampleEnrollment));
        when(enrollmentRepository.save(any(Enrollment.class))).thenReturn(sampleEnrollment);

        // Act
        assertDoesNotThrow(() -> enrollmentService.unenrollStudent("student123", "course123"));

        // Assert
        verify(enrollmentRepository).save(argThat(enrollment -> 
            "dropped".equals(enrollment.getStatus())
        ));
        verify(courseClient).decrementEnrollment("course123");
    }

    @Test
    @DisplayName("Should unenroll student by email")
    void shouldUnenrollStudentByEmail() {
        // Arrange
        String studentEmail = "student@test.com";
        when(enrollmentRepository.findByStudentIdAndCourseId(studentEmail, "course123"))
                .thenReturn(Optional.empty());
        when(enrollmentRepository.findByStudentEmailAndCourseId(studentEmail, "course123"))
                .thenReturn(Optional.of(sampleEnrollment));
        when(enrollmentRepository.save(any(Enrollment.class))).thenReturn(sampleEnrollment);

        // Act
        assertDoesNotThrow(() -> enrollmentService.unenrollStudent(studentEmail, "course123"));

        // Assert
        verify(enrollmentRepository).findByStudentEmailAndCourseId(studentEmail, "course123");
        verify(courseClient).decrementEnrollment("course123");
    }

    @Test
    @DisplayName("Should throw exception when enrollment not found for unenrollment")
    void shouldThrowExceptionWhenEnrollmentNotFoundForUnenrollment() {
        // Arrange
        when(enrollmentRepository.findByStudentIdAndCourseId("student123", "course123"))
                .thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> enrollmentService.unenrollStudent("student123", "course123"));
        
        assertTrue(exception.getMessage().contains("Failed to drop course"));
    }

    @Test
    @DisplayName("Should throw exception when course already dropped")
    void shouldThrowExceptionWhenCourseAlreadyDropped() {
        // Arrange
        Enrollment droppedEnrollment = new Enrollment();
        droppedEnrollment.setId("enrollment123");
        droppedEnrollment.setStatus("dropped");

        when(enrollmentRepository.findByStudentIdAndCourseId("student123", "course123"))
                .thenReturn(Optional.of(droppedEnrollment));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> enrollmentService.unenrollStudent("student123", "course123"));
        
        assertTrue(exception.getMessage().contains("Course is already dropped"));
    }

    @Test
    @DisplayName("Should handle course service error during unenrollment")
    void shouldHandleCourseServiceErrorDuringUnenrollment() {
        // Arrange
        when(enrollmentRepository.findByStudentIdAndCourseId("student123", "course123"))
                .thenReturn(Optional.of(sampleEnrollment));
        when(enrollmentRepository.save(any(Enrollment.class))).thenReturn(sampleEnrollment);
        doThrow(new RuntimeException("Service unavailable"))
                .when(courseClient).decrementEnrollment("course123");

        // Act - Should not throw exception even if course service fails
        assertDoesNotThrow(() -> enrollmentService.unenrollStudent("student123", "course123"));

        // Assert - Enrollment status should still be updated
        verify(enrollmentRepository).save(argThat(enrollment -> 
            "dropped".equals(enrollment.getStatus())
        ));
    }
}