package com.enrollment.course.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Demonstrates what the CORRECT security configuration should be.
 * This test shows how course endpoints SHOULD be protected.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test-correct-security")
class CorrectSecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Course endpoints SHOULD require authentication")
    void courseEndpointsShouldRequireAuthentication() throws Exception {
        // All GET endpoints should require authentication
        mockMvc.perform(get("/api/courses"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/courses/open"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/courses/123"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/courses/code/CS101"))
                .andExpect(status().isUnauthorized());

        // All POST endpoints should require authentication
        mockMvc.perform(post("/api/courses")
                .contentType("application/json")
                .content("{}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/courses/123/enroll"))
                .andExpect(status().isUnauthorized());

        // All PUT endpoints should require authentication
        mockMvc.perform(put("/api/courses/123")
                .contentType("application/json")
                .content("{}"))
                .andExpect(status().isUnauthorized());

        // All DELETE endpoints should require authentication
        mockMvc.perform(delete("/api/courses/123"))
                .andExpect(status().isUnauthorized());
    }

    /**
     * This is what the SecurityConfig SHOULD look like
     */
    @Configuration
    @EnableWebSecurity
    @EnableMethodSecurity(prePostEnabled = true)
    @Primary
    static class CorrectSecurityConfig {

        @Bean
        @Primary
        public SecurityFilterChain correctSecurityFilterChain(HttpSecurity http) throws Exception {
            http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                    // Only OPTIONS for CORS should be public
                    .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                    // Health check endpoints can be public
                    .requestMatchers("/actuator/health").permitAll()
                    // ALL course endpoints should require authentication
                    .requestMatchers("/api/courses/**").authenticated()
                    // Everything else requires authentication
                    .anyRequest().authenticated()
                )
                .sessionManagement(session -> session
                    .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                );

            return http.build();
        }
    }

    /**
     * Shows what authorization rules should be applied
     */
    @Test
    @DisplayName("Shows correct authorization rules for course management")
    void demonstrateCorrectAuthorizationRules() throws Exception {
        /*
         * Correct authorization rules:
         * 
         * 1. View courses (GET):
         *    - Students: Can view all courses
         *    - Instructors: Can view all courses
         *    - Admins: Can view all courses
         * 
         * 2. Create courses (POST):
         *    - Students: FORBIDDEN
         *    - Instructors: ALLOWED
         *    - Admins: ALLOWED
         * 
         * 3. Update courses (PUT):
         *    - Students: FORBIDDEN
         *    - Instructors: ALLOWED (only their own courses)
         *    - Admins: ALLOWED (any course)
         * 
         * 4. Delete courses (DELETE):
         *    - Students: FORBIDDEN
         *    - Instructors: FORBIDDEN (or only their own)
         *    - Admins: ALLOWED
         * 
         * 5. Enroll/Unenroll (POST):
         *    - Students: ALLOWED (with business logic validation)
         *    - Instructors: FORBIDDEN
         *    - Admins: ALLOWED
         */
        
        // This test just documents the expected behavior
        // Actual implementation would use @PreAuthorize or similar
    }
}