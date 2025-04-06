const Enrollment = require('../models/enrollment.model');
const { validationResult } = require('express-validator');

// Enroll in a course
exports.enroll = async (req, res) => {
  try {
    const errors = validationResult(req);
    if (!errors.isEmpty()) {
      return res.status(400).json({ errors: errors.array() });
    }

    const enrollment = new Enrollment({
      student: req.user.id,
      course: req.body.courseId,
      semester: req.body.semester,
      status: 'pending'
    });

    await enrollment.save();
    res.status(201).json(enrollment);
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
};

// Get enrollments
exports.getEnrollments = async (req, res) => {
  try {
    const enrollments = await Enrollment.find({
      $or: [
        { student: req.user.id },
        { course: { $in: req.user.courses } } // For faculty
      ]
    }).populate('course').populate('student', '-password');
    
    res.json(enrollments);
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
};

// Update enrollment status
exports.updateEnrollmentStatus = async (req, res) => {
  try {
    const errors = validationResult(req);
    if (!errors.isEmpty()) {
      return res.status(400).json({ errors: errors.array() });
    }

    const enrollment = await Enrollment.findByIdAndUpdate(
      req.params.id,
      { $set: { status: req.body.status } },
      { new: true, runValidators: true }
    );

    if (!enrollment) {
      return res.status(404).json({ message: 'Enrollment not found' });
    }
    
    res.json(enrollment);
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
};

// Drop enrollment
exports.dropEnrollment = async (req, res) => {
  try {
    const enrollment = await Enrollment.findOneAndUpdate(
      {
        _id: req.params.id,
        $or: [
          { student: req.user.id },
          { 'course.instructor': req.user.id }
        ]
      },
      { $set: { status: 'dropped' } },
      { new: true, runValidators: true }
    );

    if (!enrollment) {
      return res.status(404).json({ message: 'Enrollment not found or not authorized' });
    }
    
    res.json(enrollment);
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
}; 