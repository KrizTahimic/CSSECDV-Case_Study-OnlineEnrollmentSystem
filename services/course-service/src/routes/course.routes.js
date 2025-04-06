const express = require('express');
const { body } = require('express-validator');
const courseController = require('../controllers/course.controller');
const { verifyToken, authorizeRole } = require('../middleware/auth.middleware');
const router = express.Router();

// Validation middleware
const courseValidation = [
  body('code').trim().notEmpty(),
  body('title').trim().notEmpty(),
  body('description').trim().notEmpty(),
  body('credits').isInt({ min: 1 }),
  body('capacity').isInt({ min: 1 }),
  body('semester').trim().notEmpty(),
  body('schedule.days').isArray(),
  body('schedule.startTime').trim().notEmpty(),
  body('schedule.endTime').trim().notEmpty(),
  body('schedule.room').trim().notEmpty()
];

// Public routes
router.get('/', courseController.getCourses);
router.get('/:id', courseController.getCourse);

// Protected routes
router.post('/',
  verifyToken,
  authorizeRole('faculty', 'admin'),
  courseValidation,
  courseController.createCourse
);

router.put('/:id',
  verifyToken,
  authorizeRole('faculty', 'admin'),
  courseValidation,
  courseController.updateCourse
);

router.delete('/:id',
  verifyToken,
  authorizeRole('faculty', 'admin'),
  courseController.deleteCourse
);

router.patch('/:id/status',
  verifyToken,
  authorizeRole('faculty', 'admin'),
  body('status').isIn(['open', 'closed', 'cancelled']),
  courseController.updateCourseStatus
);

module.exports = router; 