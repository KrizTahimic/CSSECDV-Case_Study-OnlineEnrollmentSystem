const express = require('express');
const { body } = require('express-validator');
const enrollmentController = require('../controllers/enrollment.controller');
const { authenticateToken, authorizeRole } = require('../middleware/auth.middleware');
const router = express.Router();

// Validation middleware
const enrollmentValidation = [
  body('courseId').notEmpty(),
  body('semester').trim().notEmpty()
];

// Protected routes
router.post('/',
  authenticateToken,
  authorizeRole('student'),
  enrollmentValidation,
  enrollmentController.enroll
);

router.get('/',
  authenticateToken,
  enrollmentController.getEnrollments
);

router.patch('/:id/status',
  authenticateToken,
  authorizeRole('faculty', 'admin'),
  body('status').isIn(['pending', 'approved', 'rejected', 'dropped']),
  enrollmentController.updateEnrollmentStatus
);

router.post('/:id/drop',
  authenticateToken,
  enrollmentController.dropEnrollment
);

module.exports = router; 