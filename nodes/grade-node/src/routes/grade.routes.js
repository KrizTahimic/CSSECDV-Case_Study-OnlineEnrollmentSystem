const express = require('express');
const { body } = require('express-validator');
const gradeController = require('../controllers/grade.controller');
const { verifyToken, authorizeRole } = require('../middleware/auth.middleware');
const router = express.Router();

// Validation middleware
const gradeValidation = [
  body('studentId').notEmpty().withMessage('Student ID is required'),
  body('courseId').notEmpty().withMessage('Course ID is required'),
  body('grade').isFloat({ min: 0, max: 4.0 }).withMessage('Grade must be between 0 and 4.0'),
  body('score').isFloat({ min: 0, max: 100 }).withMessage('Score must be between 0 and 100'),
  body('comments').optional().trim()
];

const updateGradeValidation = [
  body('grade').isFloat({ min: 0, max: 4.0 }).withMessage('Grade must be between 0 and 4.0'),
  body('score').isFloat({ min: 0, max: 100 }).withMessage('Score must be between 0 and 100'),
  body('comments').optional().trim()
];

// Protected routes
router.post('/',
  verifyToken,
  authorizeRole('faculty'),
  gradeValidation,
  gradeController.submitGrade
);

router.get('/',
  verifyToken,
  gradeController.getGrades
);

router.get('/student/:studentId',
  verifyToken,
  gradeController.getStudentGrades
);

router.patch('/:id',
  verifyToken,
  authorizeRole('faculty'),
  updateGradeValidation,
  gradeController.updateGrade
);

router.delete('/:id',
  verifyToken,
  authorizeRole('faculty'),
  gradeController.deleteGrade
);

module.exports = router; 