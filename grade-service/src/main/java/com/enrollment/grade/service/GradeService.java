package com.enrollment.grade.service;

import com.enrollment.grade.model.Grade;
import com.enrollment.grade.repository.GradeRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GradeService {

    private final GradeRepository gradeRepository;

    @CircuitBreaker(name = "basic")
    public List<Grade> getAllGrades() {
        return gradeRepository.findAll();
    }

    @CircuitBreaker(name = "basic")
    public List<Grade> getStudentGrades(String studentEmail) {
        return gradeRepository.findByStudentEmail(studentEmail);
    }

    @CircuitBreaker(name = "basic")
    public List<Grade> getCourseGrades(String courseId) {
        return gradeRepository.findByCourseId(courseId);
    }

    @CircuitBreaker(name = "basic")
    public Optional<Grade> getStudentCourseGrade(String studentEmail, String courseId) {
        return gradeRepository.findByStudentEmailAndCourseId(studentEmail, courseId);
    }

    @CircuitBreaker(name = "basic")
    public List<Grade> getFacultyGrades(String facultyId) {
        return gradeRepository.findByFacultyId(facultyId);
    }

    @CircuitBreaker(name = "basic")
    public Grade submitGrade(Grade grade) {
        // Convert numerical score to letter grade
        grade.setLetterGrade(calculateLetterGrade(grade.getScore()));
        return gradeRepository.save(grade);
    }

    @CircuitBreaker(name = "basic")
    public Grade updateGrade(String id, Grade grade) {
        if (gradeRepository.existsById(id)) {
            grade.setId(id);
            grade.setLetterGrade(calculateLetterGrade(grade.getScore()));
            return gradeRepository.save(grade);
        }
        throw new RuntimeException("Grade not found");
    }

    @CircuitBreaker(name = "basic")
    public void deleteGrade(String id) {
        gradeRepository.deleteById(id);
    }

    private String calculateLetterGrade(Double score) {
        if (score >= 95) return "A";
        if (score >= 89) return "A-";
        if (score >= 83) return "B+";
        if (score >= 78) return "B";
        if (score >= 72) return "B-";
        if (score >= 66) return "C+";
        if (score >= 60) return "C";
        if (score < 60) return "F";
        return "F";
    }
} 