package com.enrollment.grade.repository;

import com.enrollment.grade.model.Grade;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GradeRepository extends MongoRepository<Grade, String> {
    List<Grade> findByStudentId(String studentId);
    List<Grade> findByCourseId(String courseId);
    Optional<Grade> findByStudentIdAndCourseId(String studentId, String courseId);
    List<Grade> findByFacultyId(String facultyId);
} 