package com.enrollment.enrollment.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Data
@Document(collection = "enrollments")
public class Enrollment {
    @Id
    private String id;

    private String studentId;
    private String courseId;
    private String status = "enrolled";
    private LocalDateTime enrollmentDate = LocalDateTime.now();

    @Transient
    private Course course;
} 