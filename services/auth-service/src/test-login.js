// test-login.js
const mongoose = require('mongoose');
const bcrypt = require('bcryptjs');
const User = require('./models/user.model');

async function testLogin() {
  try {
    console.log('Connecting to auth_service database...');
    await mongoose.connect('mongodb://localhost:27017/auth_service', {
      dbName: 'auth_service' // Force the database name
    });
    console.log('Connected to:', mongoose.connection.db.databaseName);
    
    // Test email and password
    const email = 'student1@university.edu';
    const password = 'student123';
    
    console.log(`Looking for user with email: ${email}`);
    const user = await User.findOne({ email: email.toLowerCase() });
    
    if (!user) {
      console.log('User not found in database');
      
      // Check if user exists in raw MongoDB
      const rawUser = await mongoose.connection.db.collection('users').findOne({ email: email.toLowerCase() });
      console.log('User found with raw MongoDB query:', !!rawUser);
      
      if (rawUser) {
        console.log('User exists in raw MongoDB but not through Mongoose model!');
        console.log('Raw user:', rawUser);
      }
      
      // Check all available users
      const allUsers = await User.find({});
      console.log(`Found ${allUsers.length} total users:`);
      allUsers.forEach(u => console.log(`- ${u.email}`));
      
      return;
    }
    
    console.log('User found:', {
      id: user._id,
      email: user.email,
      role: user.role
    });
    
    // Test password
    console.log('Comparing passwords...');
    const isPasswordValid = await user.comparePassword(password);
    console.log('Password valid:', isPasswordValid ? 'Yes' : 'No');
    
    console.log('Login test completed');
    
  } catch (error) {
    console.error('Error:', error);
  } finally {
    await mongoose.disconnect();
    console.log('Disconnected from MongoDB');
  }
}

testLogin();