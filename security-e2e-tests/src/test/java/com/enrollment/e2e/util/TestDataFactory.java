package com.enrollment.e2e.util;

import lombok.experimental.UtilityClass;

import java.util.Map;
import java.util.UUID;

/**
 * Factory class for creating test data objects.
 * Provides consistent test data across all e2e tests.
 */
@UtilityClass
public class TestDataFactory {
    
    public static Map<String, Object> createStudentRegistration() {
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);
        return Map.of(
            "email", "student" + uniqueId + "@test.com",
            "password", "SecurePass123!",
            "firstName", "Test",
            "lastName", "Student",
            "role", "student",
            "securityQuestion", "What is your favorite book?",
            "securityAnswer", "Clean Code"
        );
    }
    
    public static Map<String, Object> createFacultyRegistration() {
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);
        return Map.of(
            "email", "faculty" + uniqueId + "@test.com",
            "password", "SecurePass123!",
            "firstName", "Test",
            "lastName", "Faculty",
            "role", "faculty",
            "securityQuestion", "What is your favorite book?",
            "securityAnswer", "Design Patterns"
        );
    }
    
    public static Map<String, Object> createAdminRegistration() {
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);
        return Map.of(
            "email", "admin" + uniqueId + "@test.com",
            "password", "SecurePass123!",
            "firstName", "Test",
            "lastName", "Admin",
            "role", "admin",
            "securityQuestion", "What is your favorite book?",
            "securityAnswer", "The Pragmatic Programmer"
        );
    }
    
    public static Map<String, Object> createLoginRequest(String email, String password) {
        return Map.of(
            "email", email,
            "password", password
        );
    }
    
    public static Map<String, Object> createCourse(String facultyEmail) {
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);
        return Map.of(
            "courseCode", "CS" + uniqueId,
            "courseName", "Test Course " + uniqueId,
            "credits", 3,
            "capacity", 30,
            "enrolledStudents", 0,
            "instructor", Map.of(
                "instructorId", facultyEmail,
                "name", "Test Faculty",
                "email", facultyEmail
            ),
            "schedule", Map.of(
                "days", "MWF",
                "time", "10:00-11:00",
                "room", "Room 101"
            )
        );
    }
    
    public static Map<String, Object> createEnrollment(String studentEmail, String courseId) {
        return Map.of(
            "studentEmail", studentEmail,
            "courseId", courseId
        );
    }
    
    public static Map<String, Object> createGrade(String studentEmail, String courseId, double score) {
        return Map.of(
            "studentEmail", studentEmail,
            "courseId", courseId,
            "score", score,
            "comments", "Test grade submission"
        );
    }
    
    public static Map<String, Object> createPasswordChangeRequest(String currentPassword, String newPassword) {
        return Map.of(
            "currentPassword", currentPassword,
            "newPassword", newPassword
        );
    }
    
    public static Map<String, Object> createReauthRequest(String email, String password) {
        return Map.of(
            "email", email,
            "password", password
        );
    }
}