const express = require('express');
const courseController = require('../controllers/course.controller');
const { verifyToken } = require('../middleware/auth.middleware');
const router = express.Router();

// Public routes
router.get('/', verifyToken, courseController.getCourses);
router.get('/:id', verifyToken, courseController.getCourse);

module.exports = router; 