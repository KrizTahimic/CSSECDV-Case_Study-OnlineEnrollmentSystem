const express = require('express');
const { body } = require('express-validator');
const enrollmentController = require('../controllers/enrollment.controller');
const { verifyToken, authorizeRole } = require('../middleware/auth.middleware');
const router = express.Router();

// Validation middleware
const enrollmentValidation = [
  body('courseId').notEmpty(),
  body('semester').trim().notEmpty()
];

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
  authorizeRole('faculty', 'admin'),
  body('status').isIn(['pending', 'approved', 'rejected', 'dropped']),
  enrollmentController.updateEnrollmentStatus
);

router.post('/:id/drop',
  verifyToken,
  enrollmentController.dropEnrollment
);

module.exports = router; 