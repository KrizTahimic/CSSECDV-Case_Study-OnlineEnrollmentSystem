package com.enrollment.grade.controller;

import com.enrollment.grade.model.Grade;
import com.enrollment.grade.service.GradeService;
import com.enrollment.grade.security.JwtAuthenticationFilter;
import com.enrollment.grade.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.hasSize;

/**
 * Unit tests for GradeController.
 * Tests controller logic with mocked service layer and disabled security.
 */
@WebMvcTest(value = GradeController.class)
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = {
    "jwt.secret=dGVzdHNlY3JldDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDEyMzQ1Njc4OTA=",
    "jwt.expiration=86400000"
})
class GradeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private GradeService gradeService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private JwtService jwtService;

    private Grade sampleGrade;
    private String validJwtToken;

    @BeforeEach
    void setUp() {
        // Setup sample grade
        sampleGrade = new Grade();
        sampleGrade.setId("grade123");
        sampleGrade.setStudentEmail("student@test.com");
        sampleGrade.setCourseId("course123");
        sampleGrade.setScore(85.5);
        sampleGrade.setLetterGrade("B");
        sampleGrade.setSubmissionDate(LocalDateTime.now());
        sampleGrade.setFacultyId("faculty@test.com");
        sampleGrade.setComments("Good work");

        // Create a valid JWT token for testing
        String secretKey = "dGVzdHNlY3JldDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDEyMzQ1Njc4OTA=";
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        Key key = Keys.hmacShaKeyFor(keyBytes);
        
        String token = Jwts.builder()
                .setSubject("faculty@test.com")
                .claim("role", "faculty")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 86400000))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
        
        validJwtToken = "Bearer " + token;
    }

    @Test
    @DisplayName("Should get all grades")
    void shouldGetAllGrades() throws Exception {
        List<Grade> grades = Arrays.asList(sampleGrade);
        when(gradeService.getAllGrades()).thenReturn(grades);

        mockMvc.perform(get("/api/grades"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value("grade123"))
                .andExpect(jsonPath("$[0].studentEmail").value("student@test.com"))
                .andExpect(jsonPath("$[0].courseId").value("course123"))
                .andExpect(jsonPath("$[0].score").value(85.5))
                .andExpect(jsonPath("$[0].letterGrade").value("B"));

        verify(gradeService).getAllGrades();
    }

    @Test
    @DisplayName("Should get grades by student email")
    void shouldGetGradesByStudentEmail() throws Exception {
        List<Grade> grades = Arrays.asList(sampleGrade);
        when(gradeService.getStudentGrades("student@test.com")).thenReturn(grades);

        mockMvc.perform(get("/api/grades/student/student@test.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].studentEmail").value("student@test.com"));

        verify(gradeService).getStudentGrades("student@test.com");
    }

    @Test
    @DisplayName("Should get grades by course ID")
    void shouldGetGradesByCourseId() throws Exception {
        List<Grade> grades = Arrays.asList(sampleGrade);
        when(gradeService.getCourseGrades("course123")).thenReturn(grades);

        mockMvc.perform(get("/api/grades/course/course123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].courseId").value("course123"));

        verify(gradeService).getCourseGrades("course123");
    }

    @Test
    @DisplayName("Should get specific student course grade")
    void shouldGetStudentCourseGrade() throws Exception {
        when(gradeService.getStudentCourseGrade("student@test.com", "course123"))
                .thenReturn(Optional.of(sampleGrade));

        mockMvc.perform(get("/api/grades/student/student@test.com/course/course123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("grade123"))
                .andExpect(jsonPath("$.studentEmail").value("student@test.com"))
                .andExpect(jsonPath("$.courseId").value("course123"));

        verify(gradeService).getStudentCourseGrade("student@test.com", "course123");
    }

    @Test
    @DisplayName("Should return 404 when student course grade not found")
    void shouldReturn404WhenGradeNotFound() throws Exception {
        when(gradeService.getStudentCourseGrade("student@test.com", "course123"))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/api/grades/student/student@test.com/course/course123"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should get grades by faculty ID")
    void shouldGetGradesByFacultyId() throws Exception {
        List<Grade> grades = Arrays.asList(sampleGrade);
        when(gradeService.getFacultyGrades("faculty@test.com")).thenReturn(grades);

        mockMvc.perform(get("/api/grades/faculty/faculty@test.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].facultyId").value("faculty@test.com"));

        verify(gradeService).getFacultyGrades("faculty@test.com");
    }

    @Test
    @DisplayName("Should submit grade")
    void shouldSubmitGrade() throws Exception {
        Grade gradeToSubmit = new Grade();
        gradeToSubmit.setStudentEmail("student@test.com");
        gradeToSubmit.setCourseId("course123");
        gradeToSubmit.setScore(90.0);

        when(gradeService.submitGrade(org.mockito.ArgumentMatchers.any(Grade.class))).thenReturn(sampleGrade);

        mockMvc.perform(post("/api/grades")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(gradeToSubmit)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("grade123"))
                .andExpect(jsonPath("$.letterGrade").value("B"));

        verify(gradeService).submitGrade(org.mockito.ArgumentMatchers.any(Grade.class));
    }

    @Test
    @DisplayName("Should update grade")
    void shouldUpdateGrade() throws Exception {
        Grade updatedGrade = new Grade();
        updatedGrade.setScore(95.0);
        updatedGrade.setComments("Excellent work");

        when(gradeService.updateGrade(eq("grade123"), org.mockito.ArgumentMatchers.any(Grade.class))).thenReturn(sampleGrade);

        mockMvc.perform(put("/api/grades/grade123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedGrade)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("grade123"));

        verify(gradeService).updateGrade(eq("grade123"), org.mockito.ArgumentMatchers.any(Grade.class));
    }

    @Test
    @DisplayName("Should handle update grade not found")
    void shouldHandleUpdateGradeNotFound() throws Exception {
        Grade updatedGrade = new Grade();
        updatedGrade.setScore(95.0);

        when(gradeService.updateGrade(eq("grade123"), org.mockito.ArgumentMatchers.any(Grade.class)))
                .thenThrow(new RuntimeException("Grade not found"));

        mockMvc.perform(put("/api/grades/grade123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedGrade)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should delete grade")
    void shouldDeleteGrade() throws Exception {
        doNothing().when(gradeService).deleteGrade("grade123");

        mockMvc.perform(delete("/api/grades/grade123"))
                .andExpect(status().isOk());

        verify(gradeService).deleteGrade("grade123");
    }

    @Test
    @DisplayName("Should return empty list when no grades found")
    void shouldReturnEmptyListWhenNoGradesFound() throws Exception {
        when(gradeService.getStudentGrades("student@test.com")).thenReturn(Arrays.asList());

        mockMvc.perform(get("/api/grades/student/student@test.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }
}