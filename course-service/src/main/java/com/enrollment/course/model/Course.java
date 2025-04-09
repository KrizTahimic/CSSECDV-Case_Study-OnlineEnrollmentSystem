package com.enrollment.course.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

@Data
@Document(collection = "courses")
public class Course {
    @Id
    private String id;

    @Indexed(unique = true)
    private String code;

    private String title;
    private String description;
    private Integer credits;
    private Integer capacity;
    private Integer enrolled;
    private Schedule schedule;
    private String status;
    private String instructorId;
    private Instructor instructor;
} 