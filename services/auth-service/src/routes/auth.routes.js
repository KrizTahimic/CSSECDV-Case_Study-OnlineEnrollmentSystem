const express = require('express');
const { body } = require('express-validator');
const authController = require('../controllers/auth.controller');
const User = require('../models/user.model'); // Add this import for the debug route
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

// Debug routes - for testing purposes only
router.get('/debug/test-email', async (req, res) => {
  try {
    const testEmail = "student1@university.edu";
    
    // Check with Mongoose
    const userByMongoose = await User.findOne({ email: testEmail });
    
    // Check with MongoDB native driver
    const { MongoClient } = require('mongodb');
    const client = new MongoClient(process.env.MONGODB_URI || "mongodb://localhost:27017/auth_service");
    await client.connect();
    const db = client.db();
    const userByMongo = await db.collection('users').findOne({ email: testEmail });
    await client.close();
    
    res.json({
      testEmail,
      mongooseFound: !!userByMongoose,
      mongooseUser: userByMongoose ? {
        id: userByMongoose._id,
        email: userByMongoose.email,
        role: userByMongoose.role
      } : null,
      mongoFound: !!userByMongo,
      mongoUser: userByMongo ? {
        id: userByMongo._id,
        email: userByMongo.email,
        role: userByMongo.role
      } : null
    });
  } catch (error) {
    console.error('Debug error:', error);
    res.status(500).json({ error: error.message });
  }
});

// Add this comprehensive debug route
router.get('/debug/database', async (req, res) => {
  try {
    // Get all users
    const allUsers = await User.find();
    
    // Get database info
    const dbName = User.db.name;
    const collectionName = User.collection.name;
    
    res.json({
      databaseName: dbName,
      collectionName: collectionName,
      userCount: allUsers.length,
      users: allUsers.map(u => ({
        id: u._id,
        email: u.email,
        role: u.role,
        firstName: u.firstName,
        lastName: u.lastName
      }))
    });
  } catch (error) {
    console.error('Database debug error:', error);
    res.status(500).json({ error: error.message });
  }
});

// Add this to your auth.routes.js
router.get('/debug/connection', (req, res) => {
  const mongoose = require('mongoose');
  
  res.json({
    readyState: mongoose.connection.readyState,
    connected: mongoose.connection.readyState === 1,
    host: mongoose.connection.host,
    name: mongoose.connection.name,
    models: Object.keys(mongoose.models)
  });
});

// Export the router AFTER defining all routes
module.exports = router;