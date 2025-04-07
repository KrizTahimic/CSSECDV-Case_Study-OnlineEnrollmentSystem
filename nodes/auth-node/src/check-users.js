const mongoose = require('mongoose');
const User = require('./models/user.model');


//testing purposes only
async function checkUsers() {
  try {
    console.log('Connecting to auth database...');
    await mongoose.connect('mongodb://localhost:27017/auth_service');
    console.log('Connected successfully!');
    
    // Find all users
    const users = await User.find({});
    console.log(`Found ${users.length} users in the database:`);
    
    users.forEach(user => {
      console.log(`- Email: ${user.email}`);
      console.log(`  Role: ${user.role}`);
      console.log(`  First Name: ${user.firstName}`);
      console.log(`  Last Name: ${user.lastName}`);
      console.log(`  ID: ${user._id}`);
      console.log(`  Password: ${user.password}`);
      console.log('---');
    });
    
    // Try to find specific user
    const adminUser = await User.findOne({ email: 'admin@university.edu' });
    console.log('Admin user exists:', !!adminUser);
    
    const studentUser = await User.findOne({ email: 'student1@university.edu' });
    console.log('Student user exists:', !!studentUser);
    
  } catch (error) {
    console.error('Error:', error);
  } finally {
    await mongoose.disconnect();
    console.log('Disconnected from MongoDB');
  }
}

checkUsers();