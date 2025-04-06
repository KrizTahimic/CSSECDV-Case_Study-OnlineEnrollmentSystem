const express = require('express');
const { body } = require('express-validator');
const gradeController = require('../controllers/grade.controller');
const { verifyToken, authorizeRole } = require('../middleware/auth.middleware');
const router = express.Router();

// Validation middleware
const gradeValidation = [
  body('studentId').notEmpty(),
  body('courseId').notEmpty(),
  body('value').isIn(['A', 'A-', 'B+', 'B', 'B-', 'C+', 'C', 'C-', 'D+', 'D', 'F', 'I', 'W'])
];

// Protected routes
router.post('/',
  verifyToken,
  authorizeRole('faculty', 'admin'),
  gradeValidation,
  gradeController.submitGrade
);

router.get('/',
  verifyToken,
  gradeController.getGrades
);

router.patch('/:id',
  verifyToken,
  authorizeRole('faculty', 'admin'),
  body('value').isIn(['A', 'A-', 'B+', 'B', 'B-', 'C+', 'C', 'C-', 'D+', 'D', 'F', 'I', 'W']),
  gradeController.updateGrade
);

router.delete('/:id',
  verifyToken,
  authorizeRole('faculty', 'admin'),
  gradeController.deleteGrade
);

module.exports = router; 