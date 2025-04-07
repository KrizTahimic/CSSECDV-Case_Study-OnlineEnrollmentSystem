package com.enrollment.grade.repository;

import com.enrollment.grade.model.Grade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GradeRepository extends JpaRepository<Grade, Long> {
    List<Grade> findByStudentId(String studentId);
    List<Grade> findByCourseId(String courseId);
    Optional<Grade> findByStudentIdAndCourseId(String studentId, String courseId);
    List<Grade> findByFacultyId(String facultyId);
} 