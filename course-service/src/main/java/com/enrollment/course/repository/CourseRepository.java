package com.enrollment.course.repository;

import com.enrollment.course.model.Course;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseRepository extends MongoRepository<Course, String> {
    Optional<Course> findByCode(String code);
    List<Course> findByInstructorId(String instructorId);
    List<Course> findByStatus(String status);
} 