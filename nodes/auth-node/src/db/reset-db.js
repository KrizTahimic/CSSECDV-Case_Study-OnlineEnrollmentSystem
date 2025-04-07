// Create reset-db.js
const mongoose = require('mongoose');
const bcrypt = require('bcryptjs');

async function resetDatabase() {
  try {
    // Connect with explicit database name
    console.log('Connecting to MongoDB...');
    await mongoose.connect('mongodb://localhost:27017/auth_service');
    console.log('Connected to database:', mongoose.connection.db.databaseName);
    
    // Drop the database to start fresh
    console.log('Dropping database...');
    await mongoose.connection.db.dropDatabase();
    console.log('Database dropped successfully');
    
    // Define user schema and model directly in this script
    const userSchema = new mongoose.Schema({
      email: { type: String, required: true, unique: true, lowercase: true },
      password: { type: String, required: true },
      role: { type: String, enum: ['student', 'faculty', 'admin'], required: true },
      firstName: { type: String, required: true },
      lastName: { type: String, required: true },
      createdAt: { type: Date, default: Date.now }
    });
    
    // Hash password before saving
    userSchema.pre('save', async function(next) {
      if (!this.isModified('password')) return next();
      try {
        const salt = await bcrypt.genSalt(10);
        this.password = await bcrypt.hash(this.password, salt);
        next();
      } catch (error) {
        next(error);
      }
    });
    
    userSchema.methods.comparePassword = async function(candidatePassword) {
      return bcrypt.compare(candidatePassword, this.password);
    };
    
    const User = mongoose.model('User', userSchema);
    
    // Create users
    console.log('Creating users...');
    await User.create([
      {
        firstName: "Admin",
        lastName: "User",
        email: "admin@university.edu",
        password: "admin123", // Will be hashed by pre-save hook
        role: "admin"
      },
      {
        firstName: "John",
        lastName: "Smith",
        email: "john.smith@university.edu",
        password: "faculty123",
        role: "faculty"
      },
      {
        firstName: "Sarah",
        lastName: "Johnson",
        email: "sarah.johnson@university.edu",
        password: "faculty123",
        role: "faculty"
      },
      {
        firstName: "Michael",
        lastName: "Brown",
        email: "michael.brown@university.edu",
        password: "faculty123",
        role: "faculty"
      },
      {
        firstName: "Student",
        lastName: "One",
        email: "student1@university.edu",
        password: "student123", // Will be hashed by pre-save hook
        role: "student"
      }
    ]);
    
    console.log('Users created successfully');
    
    // Test finding a user
    const testUser = await User.findOne({ email: 'student1@university.edu' });
    console.log('Test user found:', testUser ? 'Yes' : 'No');
    
    // Test password verification
    if (testUser) {
      const isPasswordValid = await testUser.comparePassword('student123');
      console.log('Password verification test:', isPasswordValid ? 'PASSED' : 'FAILED');
    }
    
  } catch (error) {
    console.error('Error resetting database:', error);
  } finally {
    await mongoose.disconnect();
    console.log('Disconnected from MongoDB');
  }
}

resetDatabase();