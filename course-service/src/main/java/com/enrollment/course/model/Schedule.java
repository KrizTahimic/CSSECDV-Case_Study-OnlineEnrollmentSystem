package com.enrollment.course.model;

import lombok.Data;
import java.util.List;

@Data
public class Schedule {
    private List<String> days;
    private String startTime;
    private String endTime;
    private String room;
} 