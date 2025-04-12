package com.enrollment.course.client;

import com.enrollment.course.model.Instructor;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "auth-service")
public interface AuthClient {
    @GetMapping("/api/auth/users/{id}")
    Instructor getInstructor(@PathVariable String id);

    @GetMapping("/api/auth/users")
    java.util.List<Instructor> getInstructors();
} 