package com.enrollment.enrollment.service;

import com.enrollment.enrollment.client.CourseClient;
import com.enrollment.enrollment.model.Course;
import com.enrollment.enrollment.model.Enrollment;
import com.enrollment.enrollment.repository.EnrollmentRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
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
                if (e.getMessage().contains("Connection refused") || e.getMessage().contains("Connection timed out")) {
                    throw new RuntimeException("Course service is currently unavailable. Please try again later.");
                }
                return enrollment;
            }
        }).collect(Collectors.toList());
    }

    @CircuitBreaker(name = "basic")
    public List<Enrollment> getCourseEnrollments(String courseId) {
        List<Enrollment> enrollments = enrollmentRepository.findByCourseId(courseId);
        
        // Populate course details for each enrollment
        return enrollments.stream().map(enrollment -> {
            try {
                Course course = courseClient.getCourse(enrollment.getCourseId());
                enrollment.setCourse(course);
                return enrollment;
            } catch (Exception e) {
                System.err.println("Error fetching course details for enrollment: " + e.getMessage());
                if (e.getMessage().contains("Connection refused") || e.getMessage().contains("Connection timed out")) {
                    throw new RuntimeException("Course service is currently unavailable. Please try again later.");
                }
                return enrollment;
            }
        }).collect(Collectors.toList());
    }

    @CircuitBreaker(name = "basic")
    @Transactional
    public Enrollment enrollStudent(String studentId, String courseId) {
        System.out.println("Attempting to enroll student: " + studentId + " in course: " + courseId);
        
        // Check if student is already enrolled
        Optional<Enrollment> existingEnrollmentOpt = enrollmentRepository.findByStudentIdAndCourseId(studentId, courseId);
        
        if (existingEnrollmentOpt.isPresent()) {
            Enrollment existingEnrollment = existingEnrollmentOpt.get();
            System.out.println("Found existing enrollment with status: " + existingEnrollment.getStatus());
            
            // If enrollment exists but is dropped, reactivate it
            if ("dropped".equals(existingEnrollment.getStatus())) {
                System.out.println("Reactivating previously dropped enrollment");
                existingEnrollment.setStatus("enrolled");
                existingEnrollment.setEnrollmentDate(new Date());
                
                // Increment course enrollment
                courseClient.incrementEnrollment(courseId);
                System.out.println("Incremented course enrollment count");
                
                Enrollment savedEnrollment = enrollmentRepository.save(existingEnrollment);
                System.out.println("Enrollment reactivated successfully: " + savedEnrollment.getId());
                return savedEnrollment;
            } else {
                // If enrollment exists and is not dropped, throw an error
                System.out.println("Cannot enroll: Student is already enrolled in this course");
                throw new RuntimeException("Student is already enrolled in this course");
            }
        }

        System.out.println("No existing enrollment found, creating new enrollment");
        
        // Get course details and check if it's open
        Course course = courseClient.getCourse(courseId);
        if (course == null) {
            System.out.println("Course not found: " + courseId);
            throw new RuntimeException("Course not found");
        }
        if (!"open".equals(course.getStatus())) {
            System.out.println("Course is not open for enrollment: " + course.getStatus());
            throw new RuntimeException("Course is not open for enrollment");
        }

        // Create new enrollment
        Enrollment enrollment = new Enrollment();
        enrollment.setStudentId(studentId);
        enrollment.setCourseId(courseId);

        // Increment course enrollment
        courseClient.incrementEnrollment(courseId);
        System.out.println("Incremented course enrollment count");

        Enrollment savedEnrollment = enrollmentRepository.save(enrollment);
        System.out.println("New enrollment created successfully: " + savedEnrollment.getId());
        return savedEnrollment;
    }

    @Transactional
    @CircuitBreaker(name = "basic")
    public void unenrollStudent(String studentId, String courseId) {
        try {
            System.out.println("Attempting to unenroll student: " + studentId + " from course: " + courseId);
            
            // Find the enrollment
            Optional<Enrollment> enrollmentOpt = enrollmentRepository.findByStudentIdAndCourseId(studentId, courseId);
            
            // If not found by ID, try finding by email
            if (enrollmentOpt.isEmpty()) {
                System.out.println("Enrollment not found by ID, trying to find by email");
                // Try to find by email if studentId looks like an email
                if (studentId.contains("@")) {
                    enrollmentOpt = enrollmentRepository.findByStudentEmailAndCourseId(studentId, courseId);
                }
            }
            
            if (enrollmentOpt.isEmpty()) {
                System.out.println("Enrollment still not found after trying email");
                throw new RuntimeException("Enrollment not found for student " + studentId + " and course " + courseId);
            }

            Enrollment enrollment = enrollmentOpt.get();
            System.out.println("Found enrollment: " + enrollment.getId() + " with status: " + enrollment.getStatus());
            
            // Check if already dropped
            if ("dropped".equals(enrollment.getStatus())) {
                throw new RuntimeException("Course is already dropped");
            }
            
            // Update enrollment status to dropped
            enrollment.setStatus("dropped");
            enrollmentRepository.save(enrollment);
            System.out.println("Updated enrollment status to dropped");

            // Decrement the course enrollment count
            try {
                courseClient.decrementEnrollment(courseId);
                System.out.println("Decremented course enrollment count");
            } catch (Exception e) {
                System.err.println("Error updating course enrollment count: " + e.getMessage());
                // Don't throw here as the enrollment is already updated
            }
        } catch (Exception e) {
            System.err.println("Error in unenrollStudent: " + e.getMessage());
            throw new RuntimeException("Failed to drop course: " + e.getMessage());
        }
    }
} 