const mongoose = require('mongoose');
const bcrypt = require('bcryptjs'); // or bcryptjs, whichever you're using
const User = require('../models/user.model');

async function seedUsers() {
  try {
    // Connect to MongoDB using Mongoose
    const uri = process.env.MONGODB_URI || "mongodb://localhost:27017/auth_service";
    await mongoose.connect(uri);
    console.log("Connected to MongoDB at:", uri);
    
    // Clear existing users
    const deleteResult = await User.deleteMany({});
    console.log(`Deleted ${deleteResult.deletedCount} existing users`);
    
    // Create users using the Mongoose model
    // Note: Password hashing is handled by the pre-save hook in your model
    const users = await User.create([
      {
        firstName: "Admin",
        lastName: "User",
        email: "admin@university.edu",
        password: "admin123", // Will be hashed by pre-save hook
        role: "admin"
      },
      {
        firstName: "Student",
        lastName: "One",
        email: "student1@university.edu",
        password: "student123", // Will be hashed by pre-save hook
        role: "student"
      }
    ]);
    
    console.log(`Inserted ${users.length} users`);
    console.log("Dummy accounts created successfully");
    
    // Verification - read back the inserted users
    const adminUser = await User.findOne({ email: "admin@university.edu" });
    console.log("Admin user created:", adminUser ? "Yes" : "No");
    
  } catch (err) {
    console.error("Error seeding users:", err);
  } finally {
    // Close the Mongoose connection
    await mongoose.disconnect();
    console.log("Disconnected from MongoDB");
  }
}

// Run the seed function
seedUsers();