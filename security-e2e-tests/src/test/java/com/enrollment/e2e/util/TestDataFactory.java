package com.enrollment.e2e.util;

import lombok.experimental.UtilityClass;

import java.util.HashMap;
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
        return new HashMap<>(Map.of(
            "email", "student" + uniqueId + "@test.com",
            "password", "SecurePass123!",
            "firstName", "Test",
            "lastName", "Student",
            "role", "student",
            "securityQuestion", "What is your favorite book?",
            "securityAnswer", "Clean Code"
        ));
    }
    
    public static Map<String, Object> createFacultyRegistration() {
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);
        return new HashMap<>(Map.of(
            "email", "faculty" + uniqueId + "@test.com",
            "password", "SecurePass123!",
            "firstName", "Test",
            "lastName", "Faculty",
            "role", "faculty",
            "securityQuestion", "What is your favorite book?",
            "securityAnswer", "Design Patterns"
        ));
    }
    
    public static Map<String, Object> createAdminRegistration() {
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);
        return new HashMap<>(Map.of(
            "email", "admin" + uniqueId + "@test.com",
            "password", "SecurePass123!",
            "firstName", "Test",
            "lastName", "Admin",
            "role", "admin",
            "securityQuestion", "What is your favorite book?",
            "securityAnswer", "The Pragmatic Programmer"
        ));
    }
    
    public static Map<String, Object> createLoginRequest(String email, String password) {
        return new HashMap<>(Map.of(
            "email", email,
            "password", password
        ));
    }
    
    public static Map<String, Object> createCourse(String facultyEmail) {
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);
        Map<String, Object> course = new HashMap<>();
        course.put("courseCode", "CS" + uniqueId);
        course.put("courseName", "Test Course " + uniqueId);
        course.put("credits", 3);
        course.put("capacity", 30);
        course.put("enrolledStudents", 0);
        course.put("instructor", new HashMap<>(Map.of(
            "instructorId", facultyEmail,
            "name", "Test Faculty",
            "email", facultyEmail
        )));
        course.put("schedule", new HashMap<>(Map.of(
            "days", "MWF",
            "time", "10:00-11:00",
            "room", "Room 101"
        )));
        return course;
    }
    
    public static Map<String, Object> createEnrollment(String studentEmail, String courseId) {
        return new HashMap<>(Map.of(
            "studentEmail", studentEmail,
            "courseId", courseId
        ));
    }
    
    public static Map<String, Object> createGrade(String studentEmail, String courseId, double score) {
        return new HashMap<>(Map.of(
            "studentEmail", studentEmail,
            "courseId", courseId,
            "score", score,
            "comments", "Test grade submission"
        ));
    }
    
    public static Map<String, Object> createPasswordChangeRequest(String currentPassword, String newPassword) {
        return new HashMap<>(Map.of(
            "currentPassword", currentPassword,
            "newPassword", newPassword
        ));
    }
    
    public static Map<String, Object> createReauthRequest(String email, String password) {
        return new HashMap<>(Map.of(
            "email", email,
            "password", password
        ));
    }
    
    public static Map<String, Object> createCourseData() {
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);
        return new HashMap<>(Map.of(
            "code", "CS301",
            "name", "Advanced Algorithms " + uniqueId,
            "description", "Study of advanced algorithmic techniques",
            "capacity", 30,
            "instructorId", "faculty@test.com"
        ));
    }
}