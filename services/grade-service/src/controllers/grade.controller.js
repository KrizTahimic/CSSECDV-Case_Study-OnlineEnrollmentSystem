const Grade = require('../models/grade.model');
const axios = require('axios');
const { validationResult } = require('express-validator');

const ENROLLMENT_SERVICE_URL = process.env.ENROLLMENT_SERVICE_URL || 'http://localhost:3003';

exports.submitGrade = async (req, res) => {
  try {
    const errors = validationResult(req);
    if (!errors.isEmpty()) {
      return res.status(400).json({ errors: errors.array() });
    }

    const { studentId, courseId, semester, grade, score, comments } = req.body;
    const facultyId = req.user.userId;

    // Check if student is enrolled in the course
    const enrollmentResponse = await axios.get(`${ENROLLMENT_SERVICE_URL}/api/enrollment`, {
      params: {
        student: studentId,
        course: courseId,
        semester,
        status: 'approved'
      }
    });

    if (!enrollmentResponse.data.length) {
      return res.status(400).json({ message: 'Student is not enrolled in this course' });
    }

    // Create or update grade
    const gradeData = {
      student: studentId,
      course: courseId,
      semester,
      grade,
      score,
      comments,
      submittedBy: facultyId
    };

    const existingGrade = await Grade.findOne({
      student: studentId,
      course: courseId,
      semester
    });

    if (existingGrade) {
      Object.assign(existingGrade, gradeData);
      await existingGrade.save();
      return res.json(existingGrade);
    }

    const newGrade = new Grade(gradeData);
    await newGrade.save();
    res.status(201).json(newGrade);
  } catch (error) {
    console.error('Grade submission error:', error);
    res.status(500).json({ message: 'Server error' });
  }
};

exports.getGrades = async (req, res) => {
  try {
    const { student, course, semester } = req.query;
    const query = {};

    if (student) query.student = student;
    if (course) query.course = course;
    if (semester) query.semester = semester;

    const grades = await Grade.find(query)
      .populate('student', 'firstName lastName email')
      .populate('course', 'code title')
      .populate('submittedBy', 'firstName lastName')
      .sort({ submittedAt: -1 });

    res.json(grades);
  } catch (error) {
    console.error('Grade fetch error:', error);
    res.status(500).json({ message: 'Server error' });
  }
};

exports.getStudentGrades = async (req, res) => {
  try {
    const studentId = req.params.studentId;

    // Only allow students to view their own grades or faculty/admin to view any grades
    if (req.user.role === 'student' && req.user.userId !== studentId) {
      return res.status(403).json({ message: 'Not authorized to view these grades' });
    }

    const grades = await Grade.find({ student: studentId })
      .populate('course', 'code title credits')
      .sort({ semester: -1, submittedAt: -1 });

    res.json(grades);
  } catch (error) {
    console.error('Student grades fetch error:', error);
    res.status(500).json({ message: 'Server error' });
  }
};

exports.updateGrade = async (req, res) => {
  try {
    const errors = validationResult(req);
    if (!errors.isEmpty()) {
      return res.status(400).json({ errors: errors.array() });
    }

    const { grade, score, comments } = req.body;
    const gradeId = req.params.id;

    const existingGrade = await Grade.findById(gradeId);

    if (!existingGrade) {
      return res.status(404).json({ message: 'Grade not found' });
    }

    // Only allow faculty who submitted the grade or admin to update it
    if (existingGrade.submittedBy.toString() !== req.user.userId && req.user.role !== 'admin') {
      return res.status(403).json({ message: 'Not authorized to update this grade' });
    }

    existingGrade.grade = grade;
    existingGrade.score = score;
    existingGrade.comments = comments;
    await existingGrade.save();

    res.json(existingGrade);
  } catch (error) {
    console.error('Grade update error:', error);
    res.status(500).json({ message: 'Server error' });
  }
}; 