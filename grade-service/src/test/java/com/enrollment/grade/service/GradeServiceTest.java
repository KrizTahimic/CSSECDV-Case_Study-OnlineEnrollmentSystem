package com.enrollment.grade.service;

import com.enrollment.grade.model.Grade;
import com.enrollment.grade.repository.GradeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import org.mockito.ArgumentMatchers;

/**
 * Unit tests for GradeService.
 * Tests service layer business logic with mocked repository.
 */
@ExtendWith(MockitoExtension.class)
class GradeServiceTest {

    @Mock
    private GradeRepository gradeRepository;

    @InjectMocks
    private GradeService gradeService;

    private Grade sampleGrade;

    @BeforeEach
    void setUp() {
        sampleGrade = new Grade();
        sampleGrade.setId("grade123");
        sampleGrade.setStudentEmail("student@test.com");
        sampleGrade.setCourseId("course123");
        sampleGrade.setScore(85.5);
        sampleGrade.setLetterGrade("B");
        sampleGrade.setSubmissionDate(LocalDateTime.now());
        sampleGrade.setFacultyId("faculty@test.com");
        sampleGrade.setComments("Good work");
    }

    @Test
    @DisplayName("Should get all grades")
    void shouldGetAllGrades() {
        List<Grade> grades = Arrays.asList(sampleGrade);
        when(gradeRepository.findAll()).thenReturn(grades);

        List<Grade> result = gradeService.getAllGrades();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo("grade123");
        verify(gradeRepository).findAll();
    }

    @Test
    @DisplayName("Should get grades by student email")
    void shouldGetGradesByStudentEmail() {
        List<Grade> grades = Arrays.asList(sampleGrade);
        when(gradeRepository.findByStudentEmail("student@test.com")).thenReturn(grades);

        List<Grade> result = gradeService.getStudentGrades("student@test.com");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStudentEmail()).isEqualTo("student@test.com");
        verify(gradeRepository).findByStudentEmail("student@test.com");
    }

    @Test
    @DisplayName("Should get grades by course ID")
    void shouldGetGradesByCourseId() {
        List<Grade> grades = Arrays.asList(sampleGrade);
        when(gradeRepository.findByCourseId("course123")).thenReturn(grades);

        List<Grade> result = gradeService.getCourseGrades("course123");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCourseId()).isEqualTo("course123");
        verify(gradeRepository).findByCourseId("course123");
    }

    @Test
    @DisplayName("Should get student course grade")
    void shouldGetStudentCourseGrade() {
        when(gradeRepository.findByStudentEmailAndCourseId("student@test.com", "course123"))
                .thenReturn(Optional.of(sampleGrade));

        Optional<Grade> result = gradeService.getStudentCourseGrade("student@test.com", "course123");

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo("grade123");
        verify(gradeRepository).findByStudentEmailAndCourseId("student@test.com", "course123");
    }

    @Test
    @DisplayName("Should return empty when student course grade not found")
    void shouldReturnEmptyWhenGradeNotFound() {
        when(gradeRepository.findByStudentEmailAndCourseId("student@test.com", "course123"))
                .thenReturn(Optional.empty());

        Optional<Grade> result = gradeService.getStudentCourseGrade("student@test.com", "course123");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should get grades by faculty ID")
    void shouldGetGradesByFacultyId() {
        List<Grade> grades = Arrays.asList(sampleGrade);
        when(gradeRepository.findByFacultyId("faculty@test.com")).thenReturn(grades);

        List<Grade> result = gradeService.getFacultyGrades("faculty@test.com");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getFacultyId()).isEqualTo("faculty@test.com");
        verify(gradeRepository).findByFacultyId("faculty@test.com");
    }

    @Test
    @DisplayName("Should submit grade and calculate letter grade")
    void shouldSubmitGradeAndCalculateLetterGrade() {
        Grade gradeToSubmit = new Grade();
        gradeToSubmit.setStudentEmail("student@test.com");
        gradeToSubmit.setCourseId("course123");
        gradeToSubmit.setScore(95.0);

        Grade savedGrade = new Grade();
        savedGrade.setId("new123");
        savedGrade.setStudentEmail("student@test.com");
        savedGrade.setCourseId("course123");
        savedGrade.setScore(95.0);
        savedGrade.setLetterGrade("A");

        when(gradeRepository.save(ArgumentMatchers.any(Grade.class))).thenReturn(savedGrade);

        Grade result = gradeService.submitGrade(gradeToSubmit);

        assertThat(result.getLetterGrade()).isEqualTo("A");
        verify(gradeRepository).save(argThat(grade -> 
            grade.getLetterGrade().equals("A") && grade.getScore().equals(95.0)
        ));
    }

    @Test
    @DisplayName("Should calculate correct letter grades for different scores")
    void shouldCalculateCorrectLetterGrades() {
        testLetterGradeCalculation(96.0, "A");
        testLetterGradeCalculation(92.0, "A-");
        testLetterGradeCalculation(85.0, "B+");
        testLetterGradeCalculation(80.0, "B");
        testLetterGradeCalculation(75.0, "B-");
        testLetterGradeCalculation(68.0, "C+");
        testLetterGradeCalculation(62.0, "C");
        testLetterGradeCalculation(55.0, "F");
    }

    private void testLetterGradeCalculation(Double score, String expectedLetterGrade) {
        Grade gradeToSubmit = new Grade();
        gradeToSubmit.setScore(score);

        Grade savedGrade = new Grade();
        savedGrade.setScore(score);
        savedGrade.setLetterGrade(expectedLetterGrade);

        when(gradeRepository.save(ArgumentMatchers.any(Grade.class))).thenReturn(savedGrade);

        Grade result = gradeService.submitGrade(gradeToSubmit);
        
        assertThat(result.getLetterGrade()).isEqualTo(expectedLetterGrade);
    }

    @Test
    @DisplayName("Should update existing grade")
    void shouldUpdateExistingGrade() {
        Grade updatedGrade = new Grade();
        updatedGrade.setScore(90.0);
        updatedGrade.setComments("Updated comments");

        Grade savedGrade = new Grade();
        savedGrade.setId("grade123");
        savedGrade.setScore(90.0);
        savedGrade.setLetterGrade("A-");
        savedGrade.setComments("Updated comments");

        when(gradeRepository.existsById("grade123")).thenReturn(true);
        when(gradeRepository.save(ArgumentMatchers.any(Grade.class))).thenReturn(savedGrade);

        Grade result = gradeService.updateGrade("grade123", updatedGrade);

        assertThat(result.getId()).isEqualTo("grade123");
        assertThat(result.getLetterGrade()).isEqualTo("A-");
        verify(gradeRepository).existsById("grade123");
        verify(gradeRepository).save(argThat(grade -> 
            grade.getId().equals("grade123") && 
            grade.getLetterGrade().equals("A-")
        ));
    }

    @Test
    @DisplayName("Should throw exception when updating non-existent grade")
    void shouldThrowExceptionWhenUpdatingNonExistentGrade() {
        Grade updatedGrade = new Grade();
        updatedGrade.setScore(90.0);

        when(gradeRepository.existsById("nonexistent")).thenReturn(false);

        assertThatThrownBy(() -> gradeService.updateGrade("nonexistent", updatedGrade))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Grade not found");

        verify(gradeRepository, never()).save(ArgumentMatchers.any(Grade.class));
    }

    @Test
    @DisplayName("Should delete grade")
    void shouldDeleteGrade() {
        doNothing().when(gradeRepository).deleteById("grade123");

        gradeService.deleteGrade("grade123");

        verify(gradeRepository).deleteById("grade123");
    }

    @Test
    @DisplayName("Should handle empty results gracefully")
    void shouldHandleEmptyResultsGracefully() {
        when(gradeRepository.findByStudentEmail("nonexistent@test.com")).thenReturn(Arrays.asList());
        when(gradeRepository.findByCourseId("nonexistent")).thenReturn(Arrays.asList());
        when(gradeRepository.findByFacultyId("nonexistent@test.com")).thenReturn(Arrays.asList());

        assertThat(gradeService.getStudentGrades("nonexistent@test.com")).isEmpty();
        assertThat(gradeService.getCourseGrades("nonexistent")).isEmpty();
        assertThat(gradeService.getFacultyGrades("nonexistent@test.com")).isEmpty();
    }
}