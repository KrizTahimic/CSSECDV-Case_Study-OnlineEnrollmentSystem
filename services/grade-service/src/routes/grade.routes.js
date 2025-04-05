const express = require('express');
const { body } = require('express-validator');
const gradeController = require('../controllers/grade.controller');
const { authenticateToken, authorizeRole } = require('../middleware/auth.middleware');
const router = express.Router();

// Validation middleware
const gradeValidation = [
  body('studentId').notEmpty(),
  body('courseId').notEmpty(),
  body('semester').trim().notEmpty(),
  body('grade').isIn(['A', 'A-', 'B+', 'B', 'B-', 'C+', 'C', 'C-', 'D+', 'D', 'F', 'I', 'W']),
  body('score').optional().isFloat({ min: 0, max: 100 })
];

// Protected routes
router.post('/',
  authenticateToken,
  authorizeRole('faculty', 'admin'),
  gradeValidation,
  gradeController.submitGrade
);

router.get('/',
  authenticateToken,
  gradeController.getGrades
);

router.get('/student/:studentId',
  authenticateToken,
  gradeController.getStudentGrades
);

router.put('/:id',
  authenticateToken,
  authorizeRole('faculty', 'admin'),
  [
    body('grade').isIn(['A', 'A-', 'B+', 'B', 'B-', 'C+', 'C', 'C-', 'D+', 'D', 'F', 'I', 'W']),
    body('score').optional().isFloat({ min: 0, max: 100 })
  ],
  gradeController.updateGrade
);

module.exports = router; 