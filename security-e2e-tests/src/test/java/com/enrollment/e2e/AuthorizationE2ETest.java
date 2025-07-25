package com.enrollment.e2e;

import com.enrollment.e2e.util.JwtTestUtil;
import com.enrollment.e2e.util.TestDataFactory;
import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.Map;

import static org.hamcrest.Matchers.*;

/**
 * End-to-end tests for authorization scenarios across services.
 * Tests role-based access control through complete workflows.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Authorization End-to-End Security Tests")
public class AuthorizationE2ETest extends BaseE2ETest {
    
    private String studentToken;
    private String facultyToken;
    private String adminToken;
    private String studentEmail;
    private String facultyEmail;
    private String adminEmail;
    private String courseId;
    
    @BeforeAll
    void setupUsers() {
        // Register and login all user types
        Map<String, Object> studentData = TestDataFactory.createStudentRegistration();
        Map<String, Object> facultyData = TestDataFactory.createFacultyRegistration();
        Map<String, Object> adminData = TestDataFactory.createAdminRegistration();
        
        registerUser(studentData);
        registerUser(facultyData);
        registerUser(adminData);
        
        studentEmail = (String) studentData.get("email");
        facultyEmail = (String) facultyData.get("email");
        adminEmail = (String) adminData.get("email");
        
        studentToken = loginAndGetToken(studentEmail, (String) studentData.get("password"));
        facultyToken = loginAndGetToken(facultyEmail, (String) facultyData.get("password"));
        adminToken = loginAndGetToken(adminEmail, (String) adminData.get("password"));
    }
    
    @Test
    @DisplayName("Should enforce role-based access for course management")
    void shouldEnforceRoleBasedCourseAccess() {
        // Student should NOT be able to create courses
        RestAssured
                .given()
                    .spec(createAuthenticatedRequestSpec(studentToken))
                    .body(TestDataFactory.createCourse(facultyEmail))
                .when()
                    .post(COURSE_BASE_URL + "/api/courses")
                .then()
                    .statusCode(403);
        
        // Faculty SHOULD be able to create courses
        Map<String, Object> courseResponse = RestAssured
                .given()
                    .spec(createAuthenticatedRequestSpec(facultyToken))
                    .body(TestDataFactory.createCourse(facultyEmail))
                .when()
                    .post(COURSE_BASE_URL + "/api/courses")
                .then()
                    .statusCode(201)
                    .body("courseCode", notNullValue())
                    .extract()
                    .as(Map.class);
        
        courseId = (String) courseResponse.get("id");
        
        // Admin SHOULD also be able to create courses
        RestAssured
                .given()
                    .spec(createAuthenticatedRequestSpec(adminToken))
                    .body(TestDataFactory.createCourse(adminEmail))
                .when()
                    .post(COURSE_BASE_URL + "/api/courses")
                .then()
                    .statusCode(201);
        
        // All users SHOULD be able to view courses
        RestAssured
                .given()
                    .spec(createAuthenticatedRequestSpec(studentToken))
                .when()
                    .get(COURSE_BASE_URL + "/api/courses")
                .then()
                    .statusCode(200);
    }
    
    @Test
    @DisplayName("Should complete student enrollment workflow with proper authorization")
    void shouldCompleteStudentEnrollmentWorkflow() {
        // Create a course as faculty
        Map<String, Object> courseData = TestDataFactory.createCourse(facultyEmail);
        Map<String, Object> course = RestAssured
                .given()
                    .spec(createAuthenticatedRequestSpec(facultyToken))
                    .body(courseData)
                .when()
                    .post(COURSE_BASE_URL + "/api/courses")
                .then()
                    .statusCode(201)
                    .extract()
                    .as(Map.class);
        
        String courseId = (String) course.get("id");
        
        // Student enrolls in the course
        RestAssured
                .given()
                    .spec(createAuthenticatedRequestSpec(studentToken))
                    .body(TestDataFactory.createEnrollment(studentEmail, courseId))
                .when()
                    .post(ENROLLMENT_BASE_URL + "/api/enrollments")
                .then()
                    .statusCode(201)
                    .body("studentEmail", equalTo(studentEmail))
                    .body("courseId", equalTo(courseId))
                    .body("status", equalTo("ENROLLED"));
        
        // Student views their enrollments
        RestAssured
                .given()
                    .spec(createAuthenticatedRequestSpec(studentToken))
                .when()
                    .get(ENROLLMENT_BASE_URL + "/api/enrollments/student/" + studentEmail)
                .then()
                    .statusCode(200)
                    .body("$", hasSize(1))
                    .body("[0].courseId", equalTo(courseId));
        
        // Faculty views course enrollments
        RestAssured
                .given()
                    .spec(createAuthenticatedRequestSpec(facultyToken))
                .when()
                    .get(ENROLLMENT_BASE_URL + "/api/enrollments/course/" + courseId)
                .then()
                    .statusCode(200)
                    .body("$", hasSize(1))
                    .body("[0].studentEmail", equalTo(studentEmail));
        
        // Student cannot view other students' enrollments
        RestAssured
                .given()
                    .spec(createAuthenticatedRequestSpec(studentToken))
                .when()
                    .get(ENROLLMENT_BASE_URL + "/api/enrollments/student/other@student.com")
                .then()
                    .statusCode(403);
    }
    
    @Test
    @DisplayName("Should enforce grade submission and viewing authorization")
    void shouldEnforceGradeAuthorization() {
        // Setup: Create course and enroll student
        Map<String, Object> course = RestAssured
                .given()
                    .spec(createAuthenticatedRequestSpec(facultyToken))
                    .body(TestDataFactory.createCourse(facultyEmail))
                .when()
                    .post(COURSE_BASE_URL + "/api/courses")
                .then()
                    .statusCode(201)
                    .extract()
                    .as(Map.class);
        
        String courseId = (String) course.get("id");
        
        RestAssured
                .given()
                    .spec(createAuthenticatedRequestSpec(studentToken))
                    .body(TestDataFactory.createEnrollment(studentEmail, courseId))
                .when()
                    .post(ENROLLMENT_BASE_URL + "/api/enrollments")
                .then()
                    .statusCode(201);
        
        // Student CANNOT submit grades
        RestAssured
                .given()
                    .spec(createAuthenticatedRequestSpec(studentToken))
                    .body(TestDataFactory.createGrade(studentEmail, courseId, 85.0))
                .when()
                    .post(GRADE_BASE_URL + "/api/grades")
                .then()
                    .statusCode(403);
        
        // Faculty CAN submit grades for their course
        RestAssured
                .given()
                    .spec(createAuthenticatedRequestSpec(facultyToken))
                    .body(TestDataFactory.createGrade(studentEmail, courseId, 85.0))
                .when()
                    .post(GRADE_BASE_URL + "/api/grades")
                .then()
                    .statusCode(200)
                    .body("studentEmail", equalTo(studentEmail))
                    .body("score", equalTo(85.0))
                    .body("letterGrade", equalTo("B"));
        
        // Student CAN view their own grades
        RestAssured
                .given()
                    .spec(createAuthenticatedRequestSpec(studentToken))
                .when()
                    .get(GRADE_BASE_URL + "/api/grades/student/" + studentEmail)
                .then()
                    .statusCode(200)
                    .body("$", hasSize(1))
                    .body("[0].score", equalTo(85.0));
        
        // Student CANNOT view all grades
        RestAssured
                .given()
                    .spec(createAuthenticatedRequestSpec(studentToken))
                .when()
                    .get(GRADE_BASE_URL + "/api/grades")
                .then()
                    .statusCode(403);
        
        // Faculty CAN view all grades
        RestAssured
                .given()
                    .spec(createAuthenticatedRequestSpec(facultyToken))
                .when()
                    .get(GRADE_BASE_URL + "/api/grades")
                .then()
                    .statusCode(200);
        
        // Admin CAN delete grades
        String gradeId = RestAssured
                .given()
                    .spec(createAuthenticatedRequestSpec(adminToken))
                .when()
                    .get(GRADE_BASE_URL + "/api/grades")
                .then()
                    .statusCode(200)
                    .extract()
                    .jsonPath()
                    .getString("[0].id");
        
        RestAssured
                .given()
                    .spec(createAuthenticatedRequestSpec(adminToken))
                .when()
                    .delete(GRADE_BASE_URL + "/api/grades/" + gradeId)
                .then()
                    .statusCode(200);
        
        // Faculty CANNOT delete grades
        RestAssured
                .given()
                    .spec(createAuthenticatedRequestSpec(facultyToken))
                .when()
                    .delete(GRADE_BASE_URL + "/api/grades/" + gradeId)
                .then()
                    .statusCode(403);
    }
    
    @Test
    @DisplayName("Should prevent cross-role privilege escalation")
    void shouldPreventPrivilegeEscalation() {
        // Student tries to access faculty endpoints
        RestAssured
                .given()
                    .spec(createAuthenticatedRequestSpec(studentToken))
                .when()
                    .get(ENROLLMENT_BASE_URL + "/api/enrollments") // Admin/Faculty only
                .then()
                    .statusCode(403);
        
        // Faculty tries to access admin endpoints
        // In a real system, there would be admin-specific endpoints like user management
        
        // Test with manipulated token (invalid signature)
        String invalidToken = JwtTestUtil.generateInvalidToken();
        RestAssured
                .given()
                    .spec(createAuthenticatedRequestSpec(invalidToken))
                .when()
                    .get(COURSE_BASE_URL + "/api/courses")
                .then()
                    .statusCode(403);
    }
    
    @Test
    @DisplayName("Should enforce business logic flow in enrollment")
    void shouldEnforceBusinessLogicFlow() {
        // Create a course with limited capacity
        Map<String, Object> courseData = TestDataFactory.createCourse(facultyEmail);
        courseData.put("capacity", 1); // Only 1 seat available
        
        Map<String, Object> course = RestAssured
                .given()
                    .spec(createAuthenticatedRequestSpec(facultyToken))
                    .body(courseData)
                .when()
                    .post(COURSE_BASE_URL + "/api/courses")
                .then()
                    .statusCode(201)
                    .extract()
                    .as(Map.class);
        
        String limitedCourseId = (String) course.get("id");
        
        // First student enrolls successfully
        RestAssured
                .given()
                    .spec(createAuthenticatedRequestSpec(studentToken))
                    .body(TestDataFactory.createEnrollment(studentEmail, limitedCourseId))
                .when()
                    .post(ENROLLMENT_BASE_URL + "/api/enrollments")
                .then()
                    .statusCode(201);
        
        // Create and login second student
        Map<String, Object> student2Data = TestDataFactory.createStudentRegistration();
        registerUser(student2Data);
        String student2Token = loginAndGetToken(
            (String) student2Data.get("email"), 
            (String) student2Data.get("password")
        );
        
        // Second student should fail to enroll (course full)
        RestAssured
                .given()
                    .spec(createAuthenticatedRequestSpec(student2Token))
                    .body(TestDataFactory.createEnrollment(
                        (String) student2Data.get("email"), limitedCourseId))
                .when()
                    .post(ENROLLMENT_BASE_URL + "/api/enrollments")
                .then()
                    .statusCode(400)
                    .body("error", containsString("full"));
        
        // Student cannot drop another student's enrollment
        RestAssured
                .given()
                    .spec(createAuthenticatedRequestSpec(student2Token))
                .when()
                    .delete(ENROLLMENT_BASE_URL + "/api/enrollments/" + limitedCourseId)
                .then()
                    .statusCode(403);
        
        // Original student drops the course
        RestAssured
                .given()
                    .spec(createAuthenticatedRequestSpec(studentToken))
                .when()
                    .delete(ENROLLMENT_BASE_URL + "/api/enrollments/" + limitedCourseId)
                .then()
                    .statusCode(200);
        
        // Now second student can enroll
        RestAssured
                .given()
                    .spec(createAuthenticatedRequestSpec(student2Token))
                    .body(TestDataFactory.createEnrollment(
                        (String) student2Data.get("email"), limitedCourseId))
                .when()
                    .post(ENROLLMENT_BASE_URL + "/api/enrollments")
                .then()
                    .statusCode(201);
    }
    
    @Test
    @DisplayName("Should maintain authorization across service boundaries")
    void shouldMaintainAuthorizationAcrossServices() {
        // This test verifies that authorization is consistently enforced
        // across all microservices when accessing related resources
        
        // Create course as faculty
        Map<String, Object> course = RestAssured
                .given()
                    .spec(createAuthenticatedRequestSpec(facultyToken))
                    .body(TestDataFactory.createCourse(facultyEmail))
                .when()
                    .post(COURSE_BASE_URL + "/api/courses")
                .then()
                    .statusCode(201)
                    .extract()
                    .as(Map.class);
        
        String courseId = (String) course.get("id");
        
        // Student enrolls
        RestAssured
                .given()
                    .spec(createAuthenticatedRequestSpec(studentToken))
                    .body(TestDataFactory.createEnrollment(studentEmail, courseId))
                .when()
                    .post(ENROLLMENT_BASE_URL + "/api/enrollments")
                .then()
                    .statusCode(201);
        
        // Faculty submits grade
        RestAssured
                .given()
                    .spec(createAuthenticatedRequestSpec(facultyToken))
                    .body(TestDataFactory.createGrade(studentEmail, courseId, 90.0))
                .when()
                    .post(GRADE_BASE_URL + "/api/grades")
                .then()
                    .statusCode(200);
        
        // Verify authorization is maintained when accessing across services:
        
        // 1. Course Service - Student can view course details
        RestAssured
                .given()
                    .spec(createAuthenticatedRequestSpec(studentToken))
                .when()
                    .get(COURSE_BASE_URL + "/api/courses/" + courseId)
                .then()
                    .statusCode(200);
        
        // 2. Enrollment Service - Student can view their enrollment
        RestAssured
                .given()
                    .spec(createAuthenticatedRequestSpec(studentToken))
                .when()
                    .get(ENROLLMENT_BASE_URL + "/api/enrollments/student/" + studentEmail)
                .then()
                    .statusCode(200);
        
        // 3. Grade Service - Student can view their grade
        RestAssured
                .given()
                    .spec(createAuthenticatedRequestSpec(studentToken))
                .when()
                    .get(GRADE_BASE_URL + "/api/grades/student/" + studentEmail + "/course/" + courseId)
                .then()
                    .statusCode(200);
        
        // But student cannot perform privileged operations on any service
        RestAssured
                .given()
                    .spec(createAuthenticatedRequestSpec(studentToken))
                    .body(Map.of("capacity", 100))
                .when()
                    .put(COURSE_BASE_URL + "/api/courses/" + courseId)
                .then()
                    .statusCode(403);
    }
}