package com.enrollment.enrollment.service;

import com.enrollment.enrollment.client.CourseClient;
import com.enrollment.enrollment.model.Course;
import com.enrollment.enrollment.model.Enrollment;
import com.enrollment.enrollment.repository.EnrollmentRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final CourseClient courseClient;

    @CircuitBreaker(name = "basic")
    public List<Enrollment> getStudentEnrollments(String studentId) {
        // Get only enrolled (not dropped) enrollments
        List<Enrollment> enrollments = enrollmentRepository.findByStudentIdAndStatus(studentId, "enrolled");
        
        // Populate course details for each enrollment
        return enrollments.stream().map(enrollment -> {
            try {
                Course course = courseClient.getCourse(enrollment.getCourseId());
                enrollment.setCourse(course);
                return enrollment;
            } catch (Exception e) {
                System.err.println("Error fetching course details for enrollment: " + e.getMessage());
                return enrollment;
            }
        }).collect(Collectors.toList());
    }

    @CircuitBreaker(name = "basic")
    public List<Enrollment> getCourseEnrollments(String courseId) {
        return enrollmentRepository.findByCourseId(courseId);
    }

    @CircuitBreaker(name = "basic")
    @Transactional
    public Enrollment enrollStudent(String studentId, String courseId) {
        // Check if student is already enrolled
        if (enrollmentRepository.findByStudentIdAndCourseId(studentId, courseId).isPresent()) {
            throw new RuntimeException("Student is already enrolled in this course");
        }

        // Get course details and check if it's open
        Course course = courseClient.getCourse(courseId);
        if (course == null) {
            throw new RuntimeException("Course not found");
        }
        if (!"open".equals(course.getStatus())) {
            throw new RuntimeException("Course is not open for enrollment");
        }

        // Create new enrollment
        Enrollment enrollment = new Enrollment();
        enrollment.setStudentId(studentId);
        enrollment.setCourseId(courseId);

        // Increment course enrollment
        courseClient.incrementEnrollment(courseId);

        return enrollmentRepository.save(enrollment);
    }

    @CircuitBreaker(name = "basic")
    @Transactional
    public void unenrollStudent(String studentId, String courseId) {
        Enrollment enrollment = enrollmentRepository.findByStudentIdAndCourseId(studentId, courseId)
                .orElseThrow(() -> new RuntimeException("Enrollment not found"));

        if ("dropped".equals(enrollment.getStatus())) {
            throw new RuntimeException("Student is not enrolled in this course");
        }

        enrollment.setStatus("dropped");
        enrollmentRepository.save(enrollment);

        // Decrement course enrollment
        courseClient.decrementEnrollment(courseId);
    }
} 