package com.enrollment.e2e;

import com.enrollment.e2e.util.JwtTestUtil;
import com.enrollment.e2e.util.TestDataFactory;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.security.Key;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests to verify the e2e test infrastructure is working correctly.
 */
@DisplayName("Test Infrastructure Verification")
public class TestInfrastructureTest {
    
    @Test
    @DisplayName("Should generate valid JWT tokens")
    void shouldGenerateValidJwtTokens() {
        String token = JwtTestUtil.generateToken("test@example.com", "student");
        assertThat(token).isNotNull().isNotEmpty();
        
        // Verify token can be parsed
        String secret = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        Key key = Keys.hmacShaKeyFor(keyBytes);
        
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
        
        assertThat(claims.getSubject()).isEqualTo("test@example.com");
        assertThat(claims.get("role")).isEqualTo("student");
    }
    
    @Test
    @DisplayName("Should generate expired JWT tokens")
    void shouldGenerateExpiredJwtTokens() {
        String token = JwtTestUtil.generateExpiredToken("test@example.com", "faculty");
        assertThat(token).isNotNull().isNotEmpty();
        
        // Verify token is expired
        String secret = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        Key key = Keys.hmacShaKeyFor(keyBytes);
        
        assertThrows(io.jsonwebtoken.ExpiredJwtException.class, () -> {
            Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token);
        });
    }
    
    @Test
    @DisplayName("Should generate invalid JWT tokens")
    void shouldGenerateInvalidJwtTokens() {
        String token = JwtTestUtil.generateInvalidToken();
        assertThat(token).isNotNull().isNotEmpty();
        
        // Verify token has invalid signature
        String secret = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        Key key = Keys.hmacShaKeyFor(keyBytes);
        
        assertThrows(io.jsonwebtoken.security.SignatureException.class, () -> {
            Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token);
        });
    }
    
    @Test
    @DisplayName("Should create test data with unique identifiers")
    void shouldCreateTestDataWithUniqueIds() {
        Map<String, Object> student1 = TestDataFactory.createStudentRegistration();
        Map<String, Object> student2 = TestDataFactory.createStudentRegistration();
        
        assertThat(student1.get("email")).isNotEqualTo(student2.get("email"));
        assertThat(student1.get("password")).isEqualTo("SecurePass123!");
        assertThat(student1.get("role")).isEqualTo("student");
    }
    
    @Test
    @DisplayName("Should create different user types")
    void shouldCreateDifferentUserTypes() {
        Map<String, Object> student = TestDataFactory.createStudentRegistration();
        Map<String, Object> faculty = TestDataFactory.createFacultyRegistration();
        Map<String, Object> admin = TestDataFactory.createAdminRegistration();
        
        assertThat(student.get("role")).isEqualTo("student");
        assertThat(faculty.get("role")).isEqualTo("faculty");
        assertThat(admin.get("role")).isEqualTo("admin");
    }
    
    @Test
    @DisplayName("Should create course data")
    void shouldCreateCourseData() {
        Map<String, Object> course = TestDataFactory.createCourse("faculty@test.com");
        
        assertThat(course).containsKeys("courseCode", "courseName", "credits", "capacity", "instructor", "schedule");
        assertThat(course.get("capacity")).isEqualTo(30);
        assertThat(course.get("credits")).isEqualTo(3);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> instructor = (Map<String, Object>) course.get("instructor");
        assertThat(instructor.get("email")).isEqualTo("faculty@test.com");
    }
    
    @Test
    @DisplayName("Should create enrollment data")
    void shouldCreateEnrollmentData() {
        Map<String, Object> enrollment = TestDataFactory.createEnrollment("student@test.com", "CS101");
        
        assertThat(enrollment).containsKeys("studentEmail", "courseId");
        assertThat(enrollment.get("studentEmail")).isEqualTo("student@test.com");
        assertThat(enrollment.get("courseId")).isEqualTo("CS101");
    }
    
    @Test
    @DisplayName("Should create grade data")
    void shouldCreateGradeData() {
        Map<String, Object> grade = TestDataFactory.createGrade("student@test.com", "CS101", 85.5);
        
        assertThat(grade).containsKeys("studentEmail", "courseId", "score", "comments");
        assertThat(grade.get("score")).isEqualTo(85.5);
    }
}