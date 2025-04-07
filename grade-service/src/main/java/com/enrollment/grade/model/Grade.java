package com.enrollment.grade.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "grades")
public class Grade {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long studentId;

    @Column(nullable = false)
    private Long courseId;

    @Column(nullable = false)
    private Double score;

    @Column(nullable = false)
    private String letterGrade;

    @Column(nullable = false)
    private LocalDateTime submissionDate;

    @Column(nullable = false)
    private Long facultyId;

    @PrePersist
    protected void onCreate() {
        submissionDate = LocalDateTime.now();
    }
} 