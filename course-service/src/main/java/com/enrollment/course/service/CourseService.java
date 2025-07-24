package com.enrollment.course.service;

import com.enrollment.course.client.AuthClient;
import com.enrollment.course.model.Course;
import com.enrollment.course.model.Instructor;
import com.enrollment.course.repository.CourseRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
                System.out.println("Fetching instructor details for course " + course.getCode() + " with instructorId: " + course.getInstructorId());
                
                try {
                    // First try to get instructor by ID
                    Instructor instructor = authClient.getInstructor(course.getInstructorId());
                    
                    if (instructor != null && instructor.getFirstName() != null && instructor.getLastName() != null) {
                        System.out.println("Successfully fetched instructor by ID: " + instructor.getFirstName() + " " + instructor.getLastName());
                        course.setInstructor(instructor);
                        return course;
                    }
                } catch (Exception e) {
                    System.out.println("Failed to fetch instructor by ID: " + e.getMessage());
                }
                
                // If not found by ID, try to find in the list of all instructors
                System.out.println("Instructor not found by ID, trying to find in all instructors list");
                List<Instructor> instructors = authClient.getInstructors();
                if (instructors != null) {
                    System.out.println("Found " + instructors.size() + " instructors");
                    for (Instructor i : instructors) {
                        System.out.println("Checking instructor: " + i.getFirstName() + " " + i.getLastName() + 
                                         " (ID: " + i.getId() + ", Email: " + i.getEmail() + ")");
                        
                        // Try multiple ways to match the instructor
                        boolean idMatch = i.getId() != null && i.getId().equals(course.getInstructorId());
                        boolean emailMatch = i.getEmail() != null && i.getEmail().equals(course.getInstructorId());
                        boolean nameMatch = i.getFirstName() != null && i.getLastName() != null && 
                                         (i.getFirstName() + " " + i.getLastName()).equals(course.getInstructorId());
                        
                        if (idMatch || emailMatch || nameMatch) {
                            System.out.println("Found matching instructor: " + i.getFirstName() + " " + i.getLastName() + 
                                             " (matched by: " + (idMatch ? "ID" : emailMatch ? "email" : "name") + ")");
                            course.setInstructor(i);
                            return course;
                        }
                    }
                }
                
                System.err.println("Could not find instructor details for course " + course.getCode() + 
                    " with instructorId: " + course.getInstructorId());
            } else {
                System.err.println("No instructorId found for course " + course.getCode());
            }
        } catch (Exception e) {
            System.err.println("Error fetching instructor details for course " + course.getCode() + ": " + e.getMessage());
            e.printStackTrace();
        }
        return course;
    }

    @CircuitBreaker(name = "basic")
    public Course createCourse(Course course) {
        if (course == null) {
            throw new IllegalArgumentException("Course cannot be null");
        }
        
        // Validate required fields
        if (course.getCode() == null || course.getCode().trim().isEmpty()) {
            throw new IllegalArgumentException("Course code is required");
        }
        
        // Validate capacity
        if (course.getCapacity() != null && course.getCapacity() < 0) {
            throw new IllegalArgumentException("Course capacity cannot be negative");
        }
        
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

    @Transactional
    public Course decrementEnrollment(String id) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Course not found"));
        
        if (course.getEnrolled() <= 0) {
            throw new RuntimeException("Cannot decrement enrollment below zero");
        }
        
        course.setEnrolled(course.getEnrolled() - 1);
        if (course.getEnrolled() < course.getCapacity()) {
            course.setStatus("open");
        }
        return courseRepository.save(course);
    }
} 