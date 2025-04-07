require('dotenv').config();
const mongoose = require('mongoose');
const bcrypt = require('bcryptjs'); // or bcryptjs, whichever you're using
const User = require('../models/user.model');

const professors = [
  {
    firstName: 'Mary Grace',
    lastName: 'Piattos',
    email: 'marygrace.piattos@university.edu',
    password: 'professor123',
    role: 'faculty'
  },
  {
    firstName: 'Fernando',
    lastName: 'Tempura',
    email: 'fernando.tempura@university.edu',
    password: 'professor123',
    role: 'faculty'
  },
  {
    firstName: 'Carlos Miguel',
    lastName: 'Oishi',
    email: 'carlos.oishi@university.edu',
    password: 'professor123',
    role: 'faculty'
  },
  {
    firstName: 'Chippy',
    lastName: 'McDonald',
    email: 'chippy.mcdonald@university.edu',
    password: 'professor123',
    role: 'faculty'
  },
  {
    firstName: 'Dan Rick',
    lastName: 'Otso',
    email: 'danrick.otso@university.edu',
    password: 'professor123',
    role: 'faculty'
  },
  {
    firstName: 'Alan Rick',
    lastName: 'Otso',
    email: 'alanrick.otso@university.edu',
    password: 'professor123',
    role: 'faculty'
  }
];

const seedProfessors = async () => {
  try {
    await mongoose.connect(process.env.MONGODB_URI);
    console.log('Connected to MongoDB');

    // Clear existing professors
    await User.deleteMany({ role: 'faculty' });
    console.log('Cleared existing professors');

    // Hash passwords and create professors
    const hashedProfessors = await Promise.all(
      professors.map(async (prof) => ({
        ...prof,
        password: await bcrypt.hash(prof.password, 10)
      }))
    );

    await User.insertMany(hashedProfessors);
    console.log('Professors seeded successfully');

    await mongoose.connection.close();
    console.log('Database connection closed');
  } catch (error) {
    console.error('Error seeding professors:', error);
    process.exit(1);
  }
};

seedProfessors();