package com.enrollment.course.controller;

import com.enrollment.course.model.Course;
import com.enrollment.course.service.CourseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;

    @GetMapping
    public ResponseEntity<List<Course>> getAllCourses() {
        return ResponseEntity.ok(courseService.getAllCourses());
    }

    @GetMapping("/open")
    public ResponseEntity<List<Course>> getOpenCourses() {
        return ResponseEntity.ok(courseService.getOpenCourses());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Course> getCourseById(@PathVariable String id) {
        return courseService.getCourseById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/code/{code}")
    public ResponseEntity<Course> getCourseByCode(@PathVariable String code) {
        return courseService.getCourseByCode(code)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('faculty') or hasAuthority('Faculty') or hasAuthority('instructor') or hasAuthority('admin')")
    public ResponseEntity<Course> createCourse(@RequestBody Course course) {
        return ResponseEntity.ok(courseService.createCourse(course));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('faculty') or hasAuthority('Faculty') or hasAuthority('instructor') or hasAuthority('admin')")
    public ResponseEntity<Course> updateCourse(@PathVariable String id, @RequestBody Course course) {
        try {
            return ResponseEntity.ok(courseService.updateCourse(id, course));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('admin')")
    public ResponseEntity<Void> deleteCourse(@PathVariable String id) {
        courseService.deleteCourse(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/enroll")
    public ResponseEntity<Course> incrementEnrollment(@PathVariable String id) {
        try {
            return ResponseEntity.ok(courseService.incrementEnrollment(id));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/{id}/unenroll")
    public ResponseEntity<Course> decrementEnrollment(@PathVariable String id) {
        try {
            return ResponseEntity.ok(courseService.decrementEnrollment(id));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }
} 