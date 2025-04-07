package com.enrollment.enrollment.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;
import java.util.Date;

@Data
@Document(collection = "enrollments")
public class Enrollment {
    @Id
    private String id;

    private String studentId;
    private String studentEmail;
    private String courseId;
    private String status = "enrolled";
    private Date enrollmentDate = new Date();

    @Transient
    private Course course;
} 