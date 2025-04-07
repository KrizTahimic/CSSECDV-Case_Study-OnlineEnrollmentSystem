require('dotenv').config();
const mongoose = require('mongoose');
const User = require('./src/models/user.model');

async function checkUsers() {
  try {
    // Connect to MongoDB
    await mongoose.connect(process.env.MONGODB_URI || 'mongodb://localhost:27017/auth_node');
    console.log('Connected to MongoDB');
    
    // Get all users
    const users = await User.find().select('-password');
    console.log(`Found ${users.length} users:`);
    
    users.forEach(user => {
      console.log(`- ID: ${user._id}`);
      console.log(`  Name: ${user.firstName} ${user.lastName}`);
      console.log(`  Email: ${user.email}`);
      console.log(`  Role: ${user.role}`);
      console.log('---');
    });
    
    // Close the connection
    await mongoose.connection.close();
    console.log('Connection closed');
  } catch (error) {
    console.error('Error:', error);
  }
}

checkUsers(); 