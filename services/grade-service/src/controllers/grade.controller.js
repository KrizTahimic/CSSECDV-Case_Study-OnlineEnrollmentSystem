const Grade = require('../models/grade.model');
const axios = require('axios');
const { validationResult } = require('express-validator');

const ENROLLMENT_SERVICE_URL = process.env.ENROLLMENT_SERVICE_URL || 'http://localhost:3003';

// Submit a grade
exports.submitGrade = async (req, res) => {
  try {
    const errors = validationResult(req);
    if (!errors.isEmpty()) {
      return res.status(400).json({ errors: errors.array() });
    }

    const grade = new Grade({
      student: req.body.studentId,
      course: req.body.courseId,
      value: req.body.value,
      submittedBy: req.user.id
    });

    await grade.save();
    res.status(201).json(grade);
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
};

// Get grades
exports.getGrades = async (req, res) => {
  try {
    const query = {};
    
    // Students can only see their own grades
    if (req.user.role === 'student') {
      query.student = req.user.id;
    }
    // Faculty can see grades for their courses
    else if (req.user.role === 'faculty') {
      query.course = { $in: req.user.courses };
    }

    const grades = await Grade.find(query)
      .populate('student', '-password')
      .populate('course')
      .populate('submittedBy', '-password');
    
    res.json(grades);
  } catch (error) {
    res.status(500).json({ message: error.message });
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

// Update a grade
exports.updateGrade = async (req, res) => {
  try {
    const errors = validationResult(req);
    if (!errors.isEmpty()) {
      return res.status(400).json({ errors: errors.array() });
    }

    const grade = await Grade.findByIdAndUpdate(
      req.params.id,
      { 
        $set: { 
          value: req.body.value,
          submittedBy: req.user.id
        }
      },
      { new: true, runValidators: true }
    );

    if (!grade) {
      return res.status(404).json({ message: 'Grade not found' });
    }
    
    res.json(grade);
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
};

// Delete a grade
exports.deleteGrade = async (req, res) => {
  try {
    const result = await Grade.deleteOne({ _id: req.params.id });
    if (result.deletedCount === 0) {
      return res.status(404).json({ message: 'Grade not found' });
    }
    res.json({ message: 'Grade deleted' });
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
}; 