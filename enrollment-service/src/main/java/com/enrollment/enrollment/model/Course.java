package com.enrollment.enrollment.model;

import lombok.Data;

@Data
public class Course {
    private Long id;
    private String code;
    private String name;
    private String description;
    private Integer credits;
    private Integer capacity;
    private Integer enrolled;
    private Boolean isOpen;
} 