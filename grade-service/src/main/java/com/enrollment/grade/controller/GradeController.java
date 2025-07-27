package com.enrollment.grade.controller;

import com.enrollment.grade.model.Grade;
import com.enrollment.grade.service.GradeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.List;

@RestController
@RequestMapping("/api/grades")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true", 
        methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS},
        allowedHeaders = "*")
public class GradeController {

    private final GradeService gradeService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY')")
    public ResponseEntity<List<Grade>> getAllGrades() {
        return ResponseEntity.ok(gradeService.getAllGrades());
    }

    @GetMapping("/student/{studentEmail}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('FACULTY') or (hasRole('STUDENT') and #studentEmail == authentication.name)")
    public ResponseEntity<List<Grade>> getStudentGrades(@PathVariable String studentEmail) {
        return ResponseEntity.ok(gradeService.getStudentGrades(studentEmail));
    }

    @GetMapping("/course/{courseId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY')")
    public ResponseEntity<List<Grade>> getCourseGrades(@PathVariable String courseId) {
        return ResponseEntity.ok(gradeService.getCourseGrades(courseId));
    }

    @GetMapping("/student/{studentEmail}/course/{courseId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('FACULTY') or (hasRole('STUDENT') and #studentEmail == authentication.name)")
    public ResponseEntity<Grade> getStudentCourseGrade(
            @PathVariable String studentEmail,
            @PathVariable String courseId) {
        return gradeService.getStudentCourseGrade(studentEmail, courseId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/faculty/{facultyId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY')")
    public ResponseEntity<List<Grade>> getFacultyGrades(@PathVariable String facultyId) {
        return ResponseEntity.ok(gradeService.getFacultyGrades(facultyId));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY')")
    public ResponseEntity<Grade> submitGrade(@RequestBody Grade grade, Authentication authentication) {
        // Faculty can only submit grades for their own courses
        if (authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equalsIgnoreCase("ROLE_FACULTY"))) {
            grade.setFacultyId(authentication.getName());
        }
        return ResponseEntity.ok(gradeService.submitGrade(grade));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY')")
    public ResponseEntity<Grade> updateGrade(@PathVariable String id, @RequestBody Grade grade) {
        try {
            return ResponseEntity.ok(gradeService.updateGrade(id, grade));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteGrade(@PathVariable String id) {
        gradeService.deleteGrade(id);
        return ResponseEntity.ok().build();
    }
} 