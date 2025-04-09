package com.enrollment.grade.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Document(collection = "grades")
public class Grade {
    @Id
    private String id;

    private String studentId;

    private String courseId;

    private Double score;

    private String letterGrade;

    private LocalDateTime submissionDate;

    private String facultyId;
    
    private String comments;

    public Grade() {
        this.submissionDate = LocalDateTime.now();
    }
} 