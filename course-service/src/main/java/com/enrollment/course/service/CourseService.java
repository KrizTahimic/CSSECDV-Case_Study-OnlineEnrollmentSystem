package com.enrollment.course.service;

import com.enrollment.course.client.AuthClient;
import com.enrollment.course.model.Course;
import com.enrollment.course.model.Instructor;
import com.enrollment.course.repository.CourseRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseRepository courseRepository;
    private final AuthClient authClient;

    @CircuitBreaker(name = "basic")
    public List<Course> getAllCourses() {
        List<Course> courses = courseRepository.findAll();
        return courses.stream()
            .map(this::populateInstructorDetails)
            .collect(Collectors.toList());
    }

    @CircuitBreaker(name = "basic")
    public List<Course> getOpenCourses() {
        List<Course> courses = courseRepository.findByStatus("open");
        return courses.stream()
            .map(this::populateInstructorDetails)
            .collect(Collectors.toList());
    }

    @CircuitBreaker(name = "basic")
    public Optional<Course> getCourseById(String id) {
        return courseRepository.findById(id)
            .map(this::populateInstructorDetails);
    }

    @CircuitBreaker(name = "basic")
    public Optional<Course> getCourseByCode(String code) {
        return courseRepository.findByCode(code)
            .map(this::populateInstructorDetails);
    }

    private Course populateInstructorDetails(Course course) {
        try {
            if (course.getInstructorId() != null) {
                Instructor instructor = authClient.getInstructor(course.getInstructorId());
                course.setInstructor(instructor);
            }
        } catch (Exception e) {
            System.err.println("Error fetching instructor details for course " + course.getCode() + ": " + e.getMessage());
        }
        return course;
    }

    @CircuitBreaker(name = "basic")
    public Course createCourse(Course course) {
        course.setEnrolled(0);
        course.setStatus("open");
        return courseRepository.save(course);
    }

    @CircuitBreaker(name = "basic")
    public Course updateCourse(String id, Course course) {
        if (courseRepository.existsById(id)) {
            course.setId(id);
            return courseRepository.save(course);
        }
        throw new RuntimeException("Course not found");
    }

    @CircuitBreaker(name = "basic")
    public void deleteCourse(String id) {
        courseRepository.deleteById(id);
    }

    @CircuitBreaker(name = "basic")
    public Course incrementEnrollment(String id) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Course not found"));
        
        if (course.getEnrolled() >= course.getCapacity()) {
            throw new RuntimeException("Course is full");
        }
        
        course.setEnrolled(course.getEnrolled() + 1);
        if (course.getEnrolled() >= course.getCapacity()) {
            course.setStatus("closed");
        }
        
        return courseRepository.save(course);
    }

    @CircuitBreaker(name = "basic")
    public Course decrementEnrollment(String id) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Course not found"));
        
        if (course.getEnrolled() > 0) {
            course.setEnrolled(course.getEnrolled() - 1);
            course.setStatus("open");
            return courseRepository.save(course);
        }
        
        return course;
    }
} 