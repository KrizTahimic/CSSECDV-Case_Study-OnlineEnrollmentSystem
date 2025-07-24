package com.enrollment.auth.config;

import java.util.Arrays;
import java.util.List;

/**
 * Configuration for security questions used in password reset.
 * Implements requirement 2.1.9 - Password reset questions.
 * 
 * Questions are designed to have sufficiently random answers
 * to prevent easy guessing (avoiding common answers like "The Bible").
 */
public class SecurityQuestions {
    
    /**
     * List of approved security questions that provide good entropy.
     * These questions avoid common answers and are personal enough
     * to be memorable but hard to guess.
     */
    public static final List<String> QUESTIONS = Arrays.asList(
        "What is your mother's maiden name?",
        "What was the name of your first pet?",
        "What was the name of your elementary school?",
        "In what city were you born?",
        "What is your favorite movie?",
        "What was the make and model of your first car?",
        "What is the name of your favorite teacher?",
        "What was your childhood nickname?",
        "What is the name of the street you grew up on?",
        "What is your favorite book?"
    );
    
    /**
     * Validates if a given question is in the approved list.
     * 
     * @param question The question to validate
     * @return true if question is approved, false otherwise
     */
    public static boolean isValidQuestion(String question) {
        return QUESTIONS.contains(question);
    }
}