package com.enrollment.grade.controller;

import com.enrollment.grade.model.Grade;
import com.enrollment.grade.service.GradeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<List<Grade>> getAllGrades() {
        return ResponseEntity.ok(gradeService.getAllGrades());
    }

    @GetMapping("/student/{studentEmail}")
    public ResponseEntity<List<Grade>> getStudentGrades(@PathVariable String studentEmail) {
        return ResponseEntity.ok(gradeService.getStudentGrades(studentEmail));
    }

    @GetMapping("/course/{courseId}")
    public ResponseEntity<List<Grade>> getCourseGrades(@PathVariable String courseId) {
        return ResponseEntity.ok(gradeService.getCourseGrades(courseId));
    }

    @GetMapping("/student/{studentEmail}/course/{courseId}")
    public ResponseEntity<Grade> getStudentCourseGrade(
            @PathVariable String studentEmail,
            @PathVariable String courseId) {
        return gradeService.getStudentCourseGrade(studentEmail, courseId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/faculty/{facultyId}")
    public ResponseEntity<List<Grade>> getFacultyGrades(@PathVariable String facultyId) {
        return ResponseEntity.ok(gradeService.getFacultyGrades(facultyId));
    }

    @PostMapping
    public ResponseEntity<Grade> submitGrade(@RequestBody Grade grade) {
        return ResponseEntity.ok(gradeService.submitGrade(grade));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Grade> updateGrade(@PathVariable String id, @RequestBody Grade grade) {
        try {
            return ResponseEntity.ok(gradeService.updateGrade(id, grade));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGrade(@PathVariable String id) {
        gradeService.deleteGrade(id);
        return ResponseEntity.ok().build();
    }
} 