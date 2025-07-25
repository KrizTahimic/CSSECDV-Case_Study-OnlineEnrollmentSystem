package com.enrollment.e2e.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import java.security.Key;
import java.util.Date;
import java.util.Map;

/**
 * Utility class for generating JWT tokens in tests.
 * Matches the JWT configuration used across all services.
 */
public class JwtTestUtil {
    
    private static final String SECRET = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private static final long EXPIRATION = 86400000; // 24 hours
    
    private static Key getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(SECRET);
        return Keys.hmacShaKeyFor(keyBytes);
    }
    
    public static String generateToken(String email, String role) {
        return createToken(Map.of("role", role), email);
    }
    
    public static String generateExpiredToken(String email, String role) {
        return Jwts.builder()
                .setClaims(Map.of("role", role))
                .setSubject(email)
                .setIssuedAt(new Date(System.currentTimeMillis() - EXPIRATION - 1000))
                .setExpiration(new Date(System.currentTimeMillis() - 1000))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }
    
    public static String generateInvalidToken() {
        Key wrongKey = Keys.secretKeyFor(SignatureAlgorithm.HS256);
        return Jwts.builder()
                .setClaims(Map.of("role", "admin"))
                .setSubject("hacker@test.com")
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION))
                .signWith(wrongKey, SignatureAlgorithm.HS256)
                .compact();
    }
    
    private static String createToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }
}