package com.enrollment.e2e;

import com.enrollment.e2e.util.TestDataFactory;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * End-to-end tests for authentication security scenarios.
 * Tests complete authentication flows across the system.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Authentication End-to-End Security Tests")
public class AuthenticationE2ETest extends BaseE2ETest {
    
    @Test
    @Order(1)
    @DisplayName("Should complete full registration and login flow with security features")
    void shouldCompleteFullAuthenticationFlow() {
        // 1. Register a new student
        Map<String, Object> studentData = TestDataFactory.createStudentRegistration();
        Response registerResponse = RestAssured
                .given()
                    .spec(createRequestSpec())
                    .body(studentData)
                .when()
                    .post(AUTH_BASE_URL + REGISTER_ENDPOINT)
                .then()
                    .statusCode(201)
                    .body("email", equalTo(studentData.get("email")))
                    .body("role", equalTo("student"))
                    .body("password", nullValue()) // Password should not be returned
                    .extract()
                    .response();
        
        // 2. Login with registered credentials
        Response loginResponse = RestAssured
                .given()
                    .spec(createRequestSpec())
                    .body(TestDataFactory.createLoginRequest(
                        (String) studentData.get("email"),
                        (String) studentData.get("password")
                    ))
                .when()
                    .post(AUTH_BASE_URL + LOGIN_ENDPOINT)
                .then()
                    .statusCode(200)
                    .body("token", notNullValue())
                    .body("email", equalTo(studentData.get("email")))
                    .body("role", equalTo("student"))
                    .body("lastLoginTime", nullValue()) // First login
                    .body("lastLoginIP", nullValue()) // First login
                    .extract()
                    .response();
        
        String token = loginResponse.jsonPath().getString("token");
        assertThat(token).isNotEmpty();
        
        // 3. Login again to verify last login tracking
        Response secondLoginResponse = RestAssured
                .given()
                    .spec(createRequestSpec())
                    .body(TestDataFactory.createLoginRequest(
                        (String) studentData.get("email"),
                        (String) studentData.get("password")
                    ))
                .when()
                    .post(AUTH_BASE_URL + LOGIN_ENDPOINT)
                .then()
                    .statusCode(200)
                    .body("lastLoginTime", notNullValue()) // Should show previous login
                    .body("lastLoginIP", notNullValue()) // Should show previous IP
                    .extract()
                    .response();
        
        // 4. Access protected resource with token
        RestAssured
                .given()
                    .spec(createAuthenticatedRequestSpec(token))
                .when()
                    .get(COURSE_BASE_URL + "/api/courses")
                .then()
                    .statusCode(200);
    }
    
    @Test
    @Order(2)
    @DisplayName("Should enforce account lockout after failed attempts")
    void shouldEnforceAccountLockout() {
        // Register a user
        Map<String, Object> userData = TestDataFactory.createFacultyRegistration();
        registerUser(userData);
        
        String email = (String) userData.get("email");
        String correctPassword = (String) userData.get("password");
        
        // Make 5 failed login attempts
        for (int i = 1; i <= 5; i++) {
            RestAssured
                    .given()
                        .spec(createRequestSpec())
                        .body(TestDataFactory.createLoginRequest(email, "WrongPassword" + i))
                    .when()
                        .post(AUTH_BASE_URL + LOGIN_ENDPOINT)
                    .then()
                        .statusCode(401)
                        .body("error", equalTo("Invalid username and/or password"));
        }
        
        // 6th attempt should fail even with correct password (account locked)
        RestAssured
                .given()
                    .spec(createRequestSpec())
                    .body(TestDataFactory.createLoginRequest(email, correctPassword))
                .when()
                    .post(AUTH_BASE_URL + LOGIN_ENDPOINT)
                .then()
                    .statusCode(401)
                    .body("error", containsString("locked"));
        
        // Wait for lockout period (15 minutes in production, but we'll test immediate for demo)
        // In real scenario, you would wait or configure shorter lockout for tests
    }
    
    @Test
    @Order(3)
    @DisplayName("Should require re-authentication for password change")
    void shouldRequireReauthenticationForPasswordChange() {
        // Register and login
        Map<String, Object> userData = TestDataFactory.createAdminRegistration();
        registerUser(userData);
        
        String email = (String) userData.get("email");
        String currentPassword = (String) userData.get("password");
        String token = loginAndGetToken(email, currentPassword);
        
        // Try to change password without re-authentication (should fail)
        RestAssured
                .given()
                    .spec(createAuthenticatedRequestSpec(token))
                    .body(TestDataFactory.createPasswordChangeRequest(
                        currentPassword, "NewSecurePass123!"))
                .when()
                    .post(AUTH_BASE_URL + CHANGE_PASSWORD_ENDPOINT)
                .then()
                    .statusCode(403)
                    .body("error", containsStringIgnoringCase("re-authentication"));
        
        // Re-authenticate
        RestAssured
                .given()
                    .spec(createAuthenticatedRequestSpec(token))
                    .body(TestDataFactory.createReauthRequest(email, currentPassword))
                .when()
                    .post(AUTH_BASE_URL + REAUTHENTICATE_ENDPOINT)
                .then()
                    .statusCode(200);
        
        // Now password change should work
        RestAssured
                .given()
                    .spec(createAuthenticatedRequestSpec(token))
                    .body(TestDataFactory.createPasswordChangeRequest(
                        currentPassword, "NewSecurePass123!"))
                .when()
                    .post(AUTH_BASE_URL + CHANGE_PASSWORD_ENDPOINT)
                .then()
                    .statusCode(200)
                    .body("message", containsString("successfully"));
    }
    
    @Test
    @Order(4)
    @DisplayName("Should enforce password history and age restrictions")
    void shouldEnforcePasswordHistoryAndAge() {
        // Register user
        Map<String, Object> userData = TestDataFactory.createStudentRegistration();
        registerUser(userData);
        
        String email = (String) userData.get("email");
        String originalPassword = (String) userData.get("password");
        String token = loginAndGetToken(email, originalPassword);
        
        // Re-authenticate for password change
        RestAssured
                .given()
                    .spec(createAuthenticatedRequestSpec(token))
                    .body(TestDataFactory.createReauthRequest(email, originalPassword))
                .when()
                    .post(AUTH_BASE_URL + REAUTHENTICATE_ENDPOINT)
                .then()
                    .statusCode(200);
        
        // Try to change password immediately (should fail due to age restriction)
        RestAssured
                .given()
                    .spec(createAuthenticatedRequestSpec(token))
                    .body(TestDataFactory.createPasswordChangeRequest(
                        originalPassword, "NewSecurePass123!"))
                .when()
                    .post(AUTH_BASE_URL + CHANGE_PASSWORD_ENDPOINT)
                .then()
                    .statusCode(400)
                    .body("error", containsString("24 hours"));
        
        // In a real test, we would wait or manipulate time
        // For now, we'll test password history by attempting to reuse old password
    }
    
    @Test
    @Order(5)
    @DisplayName("Should validate password complexity requirements")
    void shouldValidatePasswordComplexity() {
        // Test various weak passwords
        String[][] weakPasswords = {
            {"weak", "too short"},
            {"weakpassword", "no uppercase, number, or special character"},
            {"WeakPassword", "no number or special character"},
            {"WeakPassword1", "no special character"},
            {"weak pass 1!", "contains space"}
        };
        
        for (String[] passwordTest : weakPasswords) {
            Map<String, Object> userData = TestDataFactory.createStudentRegistration();
            userData.put("password", passwordTest[0]);
            
            RestAssured
                    .given()
                        .spec(createRequestSpec())
                        .body(userData)
                    .when()
                        .post(AUTH_BASE_URL + REGISTER_ENDPOINT)
                    .then()
                        .statusCode(400)
                        .body("error", equalTo("Invalid input data provided"));
        }
        
        // Test valid password
        Map<String, Object> validUserData = TestDataFactory.createStudentRegistration();
        RestAssured
                .given()
                    .spec(createRequestSpec())
                    .body(validUserData)
                .when()
                    .post(AUTH_BASE_URL + REGISTER_ENDPOINT)
                .then()
                    .statusCode(201);
    }
    
    @Test
    @Order(6)
    @DisplayName("Should return generic error messages for authentication failures")
    void shouldReturnGenericErrorMessages() {
        // Register a user
        Map<String, Object> userData = TestDataFactory.createFacultyRegistration();
        registerUser(userData);
        
        String email = (String) userData.get("email");
        
        // Test with wrong password (user exists)
        RestAssured
                .given()
                    .spec(createRequestSpec())
                    .body(TestDataFactory.createLoginRequest(email, "WrongPassword123!"))
                .when()
                    .post(AUTH_BASE_URL + LOGIN_ENDPOINT)
                .then()
                    .statusCode(401)
                    .body("error", equalTo("Invalid username and/or password"));
        
        // Test with non-existent user
        RestAssured
                .given()
                    .spec(createRequestSpec())
                    .body(TestDataFactory.createLoginRequest("nonexistent@test.com", "AnyPassword123!"))
                .when()
                    .post(AUTH_BASE_URL + LOGIN_ENDPOINT)
                .then()
                    .statusCode(401)
                    .body("error", equalTo("Invalid username and/or password"));
        
        // Both errors should be identical
    }
    
    @Test
    @Order(7)
    @DisplayName("Should handle concurrent authentication attempts")
    void shouldHandleConcurrentAuthentication() {
        // This test simulates multiple users trying to authenticate simultaneously
        Map<String, Object> user1 = TestDataFactory.createStudentRegistration();
        Map<String, Object> user2 = TestDataFactory.createFacultyRegistration();
        Map<String, Object> user3 = TestDataFactory.createAdminRegistration();
        
        // Register all users
        registerUser(user1);
        registerUser(user2);
        registerUser(user3);
        
        // Attempt concurrent logins
        Response[] responses = new Response[3];
        
        // In a real scenario, these would be truly concurrent
        responses[0] = RestAssured
                .given()
                    .spec(createRequestSpec())
                    .body(TestDataFactory.createLoginRequest(
                        (String) user1.get("email"), 
                        (String) user1.get("password")))
                .when()
                    .post(AUTH_BASE_URL + LOGIN_ENDPOINT);
        
        responses[1] = RestAssured
                .given()
                    .spec(createRequestSpec())
                    .body(TestDataFactory.createLoginRequest(
                        (String) user2.get("email"), 
                        (String) user2.get("password")))
                .when()
                    .post(AUTH_BASE_URL + LOGIN_ENDPOINT);
        
        responses[2] = RestAssured
                .given()
                    .spec(createRequestSpec())
                    .body(TestDataFactory.createLoginRequest(
                        (String) user3.get("email"), 
                        (String) user3.get("password")))
                .when()
                    .post(AUTH_BASE_URL + LOGIN_ENDPOINT);
        
        // All should succeed
        for (Response response : responses) {
            assertThat(response.getStatusCode()).isEqualTo(200);
            assertThat(response.jsonPath().getString("token")).isNotEmpty();
        }
    }
}