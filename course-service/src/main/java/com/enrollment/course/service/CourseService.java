package com.enrollment.course.service;

import com.enrollment.course.model.Course;
import com.enrollment.course.repository.CourseRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseRepository courseRepository;

    @CircuitBreaker(name = "basic")
    public List<Course> getAllCourses() {
        return courseRepository.findAll();
    }

    @CircuitBreaker(name = "basic")
    public List<Course> getOpenCourses() {
        return courseRepository.findByIsOpenTrue();
    }

    @CircuitBreaker(name = "basic")
    public Optional<Course> getCourseById(Long id) {
        return courseRepository.findById(id);
    }

    @CircuitBreaker(name = "basic")
    public Optional<Course> getCourseByCode(String code) {
        return courseRepository.findByCode(code);
    }

    @CircuitBreaker(name = "basic")
    public Course createCourse(Course course) {
        course.setEnrolled(0);
        course.setIsOpen(true);
        return courseRepository.save(course);
    }

    @CircuitBreaker(name = "basic")
    public Course updateCourse(Long id, Course course) {
        if (courseRepository.existsById(id)) {
            course.setId(id);
            return courseRepository.save(course);
        }
        throw new RuntimeException("Course not found");
    }

    @CircuitBreaker(name = "basic")
    public void deleteCourse(Long id) {
        courseRepository.deleteById(id);
    }

    @CircuitBreaker(name = "basic")
    public Course incrementEnrollment(Long id) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Course not found"));
        
        if (course.getEnrolled() >= course.getCapacity()) {
            throw new RuntimeException("Course is full");
        }
        
        course.setEnrolled(course.getEnrolled() + 1);
        if (course.getEnrolled() >= course.getCapacity()) {
            course.setIsOpen(false);
        }
        
        return courseRepository.save(course);
    }

    @CircuitBreaker(name = "basic")
    public Course decrementEnrollment(Long id) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Course not found"));
        
        if (course.getEnrolled() > 0) {
            course.setEnrolled(course.getEnrolled() - 1);
            course.setIsOpen(true);
            return courseRepository.save(course);
        }
        
        return course;
    }
} 