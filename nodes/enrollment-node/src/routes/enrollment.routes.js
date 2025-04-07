const express = require('express');
const { body } = require('express-validator');
const enrollmentController = require('../controllers/enrollment.controller');
const { verifyToken, authorizeRole } = require('../middleware/auth.middleware');
const router = express.Router();

// Validation middleware
const enrollmentValidation = [
  body('course').notEmpty().withMessage('Course ID is required')
];

// Get enrollments by course ID (specific route first)
router.get('/course/:courseId', verifyToken, enrollmentController.getEnrollmentsByCourse);

// Check if a student is enrolled in a course
router.get('/check/:studentId/:courseId', verifyToken, enrollmentController.checkEnrollment);

// Protected routes
router.post('/',
  verifyToken,
  authorizeRole('student'),
  enrollmentValidation,
  enrollmentController.enroll
);

router.get('/',
  verifyToken,
  enrollmentController.getEnrollments
);

router.patch('/:id/status',
  verifyToken,
  authorizeRole('faculty'),
  body('status').isIn(['pending', 'approved', 'rejected', 'dropped']),
  enrollmentController.updateEnrollmentStatus
);

router.post('/:id/drop',
  verifyToken,
  enrollmentController.dropEnrollment
);

module.exports = router; 