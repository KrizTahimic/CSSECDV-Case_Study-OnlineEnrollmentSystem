const bcrypt = require('bcryptjs');
const { MongoClient } = require('mongodb');
const jwt = require('jsonwebtoken');
const User = require('../models/user.model');
const { validationResult } = require('express-validator');

const JWT_SECRET = process.env.JWT_SECRET || 'your-secret-key';
const JWT_EXPIRES_IN = '24h';

exports.register = async (req, res) => {
  try {
    const errors = validationResult(req);
    if (!errors.isEmpty()) {
      return res.status(400).json({ errors: errors.array() });
    }

    const { email, password, role, firstName, lastName } = req.body;

    // Check if user already exists
    let user = await User.findOne({ email });
    if (user) {
      return res.status(400).json({ message: 'User already exists' });
    }

    // Create new user
    user = new User({
      email,
      password,
      role,
      firstName,
      lastName
    });

    await user.save();

    // Generate JWT token
    const token = jwt.sign(
      { userId: user._id, role: user.role },
      JWT_SECRET,
      { expiresIn: JWT_EXPIRES_IN }
    );

    res.status(201).json({
      token,
      user: {
        id: user._id,
        email: user.email,
        role: user.role,
        firstName: user.firstName,
        lastName: user.lastName
      }
    });
  } catch (error) {
    console.error('Registration error:', error);
    res.status(500).json({ message: 'Server error' });
  }
};

// Correct login controller method
exports.login = async (req, res) => {
  try {
    // Check for validation errors
    const errors = validationResult(req);
    if (!errors.isEmpty()) {
      return res.status(400).json({ errors: errors.array() });
    }
    
    const { email, password } = req.body;
    console.log('Login attempt with email:', email);
    
    // Find user using Mongoose model
    const user = await User.findOne({ email: email.toLowerCase() });
    console.log('User found in database:', user ? 'Yes' : 'No');
    
    if (!user) {
      console.log('Authentication failed: User not found');
      return res.status(401).json({ message: 'Invalid credentials User' });
    }
    
    // Debug log - user details
    console.log('User details:', { 
      id: user._id, 
      email: user.email,
      role: user.role
    });
    
    // Verify password using the model method
    console.log('Comparing passwords...');
    const isPasswordValid = await user.comparePassword(password);
    console.log('Password valid:', isPasswordValid ? 'Yes' : 'No');
    
    if (!isPasswordValid) {
      console.log('Authentication failed: Invalid password');
      return res.status(401).json({ message: 'Invalid credentials Password' });
    }
    
    // Generate token
    const token = jwt.sign(
      { userId: user._id, role: user.role },
      JWT_SECRET,
      { expiresIn: JWT_EXPIRES_IN }
    );
    
    console.log('Authentication successful for user:', user.email);
    
    // Return success response
    res.status(200).json({ 
      message: 'Login successful',
      token,
      user: {
        id: user._id,
        email: user.email,
        firstName: user.firstName,
        lastName: user.lastName,
        role: user.role
      }
    });
  } catch (error) {
    console.error('Login error:', error);
    res.status(500).json({ message: 'Login failed' });
  }
};

exports.verifyToken = async (req, res) => {
  try {
    const token = req.headers.authorization?.split(' ')[1];
    if (!token) {
      return res.status(401).json({ message: 'No token provided' });
    }

    const decoded = jwt.verify(token, JWT_SECRET);
    const user = await User.findById(decoded.userId).select('-password');
    
    if (!user) {
      return res.status(401).json({ message: 'Invalid token' });
    }

    res.json({ user });
  } catch (error) {
    console.error('Token verification error:', error);
    res.status(401).json({ message: 'Invalid token' });
  }
}; 