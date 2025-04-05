const express = require('express');
const { body } = require('express-validator');
const authController = require('../controllers/auth.controller');
const router = express.Router();

// Validation middleware
const registerValidation = [
  body('email').isEmail().normalizeEmail(),
  body('password').isLength({ min: 6 }),
  body('role').isIn(['student', 'faculty', 'admin']),
  body('firstName').trim().notEmpty(),
  body('lastName').trim().notEmpty()
];

const loginValidation = [
  body('email').isEmail().normalizeEmail(),
  body('password').notEmpty()
];

// Routes
router.post('/register', registerValidation, authController.register);
router.post('/login', loginValidation, authController.login);
router.get('/verify', authController.verifyToken);

module.exports = router; 