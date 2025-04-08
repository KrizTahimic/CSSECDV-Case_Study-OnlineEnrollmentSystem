package com.enrollment.enrollment.model;

import lombok.Data;

@Data
public class Course {
    private String id;
    private String code;
    private String title;
    private String description;
    private Integer credits;
    private Integer capacity;
    private Integer enrolled;
    private String status;
    private String instructorId;
    private Instructor instructor;
} 