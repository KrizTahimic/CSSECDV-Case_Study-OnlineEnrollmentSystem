require('dotenv').config();
const mongoose = require('mongoose');
const Course = require('../models/course.model');

// Simple User schema for seeding
const userSchema = new mongoose.Schema({
  firstName: String,
  lastName: String,
  email: String,
  role: String
});

const courses = [
  {
    code: 'VAL101',
    title: 'Valorant Basics',
    description: 'Learn the fundamentals of Valorant, including agent abilities, map strategies, and team composition.',
    credits: 3,
    capacity: 30,
    enrolled: 0,
    schedule: {
      days: ['Monday', 'Wednesday', 'Friday'],
      startTime: '9:00 AM',
      endTime: '10:30 AM',
      room: 'Room 101'
    },
    status: 'open'
  },
  {
    code: 'MAR101',
    title: 'Marvel Rivals 101',
    description: 'Introduction to Marvel Rivals, covering hero mechanics, team synergies, and competitive strategies.',
    credits: 3,
    capacity: 25,
    enrolled: 0,
    schedule: {
      days: ['Tuesday', 'Thursday'],
      startTime: '1:00 PM',
      endTime: '2:30 PM',
      room: 'Room 102'
    },
    status: 'open'
  },
  {
    code: 'APX101',
    title: 'Apex Legends Academy',
    description: 'Master the art of Apex Legends, from legend abilities to advanced movement techniques.',
    credits: 3,
    capacity: 35,
    enrolled: 0,
    schedule: {
      days: ['Monday', 'Wednesday', 'Friday'],
      startTime: '2:00 PM',
      endTime: '3:30 PM',
      room: 'Room 103'
    },
    status: 'open'
  },
  {
    code: 'GEN101',
    title: 'Genshin Impact Class',
    description: 'Explore the world of Teyvat, learn about character building, and team compositions.',
    credits: 3,
    capacity: 40,
    enrolled: 0,
    schedule: {
      days: ['Tuesday', 'Thursday'],
      startTime: '9:00 AM',
      endTime: '10:30 AM',
      room: 'Room 104'
    },
    status: 'open'
  },
  {
    code: 'PAL101',
    title: 'Introduction to Palworld',
    description: 'Learn about Pal management, base building, and survival strategies in Palworld.',
    credits: 3,
    capacity: 30,
    enrolled: 0,
    schedule: {
      days: ['Monday', 'Wednesday', 'Friday'],
      startTime: '1:00 PM',
      endTime: '2:30 PM',
      room: 'Room 105'
    },
    status: 'open'
  },
  {
    code: 'PKM101',
    title: 'Pokemon School',
    description: 'Master Pokemon battling, team building, and competitive strategies.',
    credits: 3,
    capacity: 35,
    enrolled: 0,
    schedule: {
      days: ['Tuesday', 'Thursday'],
      startTime: '2:00 PM',
      endTime: '3:30 PM',
      room: 'Room 106'
    },
    status: 'open'
  }
];

const seedCourses = async () => {
  try {
    // Connect to both databases
    await mongoose.connect(process.env.MONGODB_URI);
    console.log('Connected to Course MongoDB');

    const authMongoose = mongoose.createConnection(process.env.AUTH_MONGODB_URI || 'mongodb://localhost:27017/auth_node');
    console.log('Connected to Auth MongoDB');

    // Get all professors
    const Professor = authMongoose.model('User', userSchema);
    const professors = await Professor.find({ role: 'faculty' });
    
    if (professors.length === 0) {
      console.error('No professors found. Please seed professors first.');
      process.exit(1);
    }

    // Clear existing courses
    await Course.deleteMany({});
    console.log('Cleared existing courses');

    // Assign professors to courses
    const coursesWithInstructors = courses.map((course, index) => ({
      ...course,
      instructor: professors[index % professors.length]._id
    }));

    await Course.insertMany(coursesWithInstructors);
    console.log('Courses seeded successfully');

    await mongoose.connection.close();
    await authMongoose.close();
    console.log('Database connections closed');
  } catch (error) {
    console.error('Error seeding courses:', error);
    process.exit(1);
  }
};

seedCourses(); 