package com.enrollment.enrollment.client;

import com.enrollment.enrollment.model.Course;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "course-service", configuration = com.enrollment.enrollment.config.FeignConfig.class)
public interface CourseClient {
    @GetMapping("/api/courses/{id}")
    Course getCourse(@PathVariable String id);

    @PostMapping("/api/courses/{id}/enroll")
    Course incrementEnrollment(@PathVariable String id);

    @PostMapping("/api/courses/{id}/unenroll")
    Course decrementEnrollment(@PathVariable String id);
} 