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
    public List<Grade> getStudentGrades(Long studentId) {
        return gradeRepository.findByStudentId(studentId);
    }

    @CircuitBreaker(name = "basic")
    public List<Grade> getCourseGrades(Long courseId) {
        return gradeRepository.findByCourseId(courseId);
    }

    @CircuitBreaker(name = "basic")
    public Optional<Grade> getStudentCourseGrade(Long studentId, Long courseId) {
        return gradeRepository.findByStudentIdAndCourseId(studentId, courseId);
    }

    @CircuitBreaker(name = "basic")
    public List<Grade> getFacultyGrades(Long facultyId) {
        return gradeRepository.findByFacultyId(facultyId);
    }

    @CircuitBreaker(name = "basic")
    public Grade submitGrade(Grade grade) {
        // Convert numerical score to letter grade
        grade.setLetterGrade(calculateLetterGrade(grade.getScore()));
        return gradeRepository.save(grade);
    }

    @CircuitBreaker(name = "basic")
    public Grade updateGrade(Long id, Grade grade) {
        if (gradeRepository.existsById(id)) {
            grade.setId(id);
            grade.setLetterGrade(calculateLetterGrade(grade.getScore()));
            return gradeRepository.save(grade);
        }
        throw new RuntimeException("Grade not found");
    }

    @CircuitBreaker(name = "basic")
    public void deleteGrade(Long id) {
        gradeRepository.deleteById(id);
    }

    private String calculateLetterGrade(Double score) {
        if (score >= 93) return "A";
        if (score >= 90) return "A-";
        if (score >= 87) return "B+";
        if (score >= 83) return "B";
        if (score >= 80) return "B-";
        if (score >= 77) return "C+";
        if (score >= 73) return "C";
        if (score >= 70) return "C-";
        if (score >= 67) return "D+";
        if (score >= 63) return "D";
        if (score >= 60) return "D-";
        return "F";
    }
} 