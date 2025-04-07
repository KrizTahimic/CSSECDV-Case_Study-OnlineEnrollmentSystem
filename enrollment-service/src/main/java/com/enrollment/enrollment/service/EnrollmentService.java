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

@Service
@RequiredArgsConstructor
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final CourseClient courseClient;

    @CircuitBreaker(name = "basic")
    public List<Enrollment> getStudentEnrollments(Long studentId) {
        return enrollmentRepository.findByStudentIdAndActive(studentId, true);
    }

    @CircuitBreaker(name = "basic")
    public List<Enrollment> getCourseEnrollments(Long courseId) {
        return enrollmentRepository.findByCourseId(courseId);
    }

    @CircuitBreaker(name = "basic")
    @Transactional
    public Enrollment enrollStudent(Long studentId, Long courseId) {
        // Check if student is already enrolled
        Optional<Enrollment> existingEnrollment = enrollmentRepository.findByStudentIdAndCourseId(studentId, courseId);
        if (existingEnrollment.isPresent() && existingEnrollment.get().getActive()) {
            throw new RuntimeException("Student is already enrolled in this course");
        }

        // Get course details and check if it's open
        Course course = courseClient.getCourse(courseId);
        if (!course.getIsOpen()) {
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
    public void unenrollStudent(Long studentId, Long courseId) {
        Enrollment enrollment = enrollmentRepository.findByStudentIdAndCourseId(studentId, courseId)
                .orElseThrow(() -> new RuntimeException("Enrollment not found"));

        if (!enrollment.getActive()) {
            throw new RuntimeException("Student is not enrolled in this course");
        }

        enrollment.setActive(false);
        enrollmentRepository.save(enrollment);

        // Decrement course enrollment
        courseClient.decrementEnrollment(courseId);
    }
} 