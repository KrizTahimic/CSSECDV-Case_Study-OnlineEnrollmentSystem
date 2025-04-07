require('dotenv').config();
const express = require('express');
const mongoose = require('mongoose');
const cors = require('cors');
const enrollmentRoutes = require('./routes/enrollment.routes');

const app = express();
const PORT = process.env.PORT || 3003;

// CORS configuration
const corsOptions = {
  origin: ['http://localhost:3000', 'http://127.0.0.1:3000'],
  methods: ['GET', 'POST', 'PUT', 'DELETE', 'PATCH'],
  allowedHeaders: ['Content-Type', 'Authorization'],
  credentials: true,
  optionsSuccessStatus: 200
};

// Middleware
app.use(cors(corsOptions));
app.use(express.json());

// Create connections to databases
const enrollmentDB = mongoose.createConnection(process.env.MONGODB_URI || 'mongodb://localhost:27017/enrollment_node');
const authDB = mongoose.createConnection(process.env.AUTH_MONGODB_URI || 'mongodb://localhost:27017/auth_node');
const courseDB = mongoose.createConnection(process.env.COURSE_MONGODB_URI || 'mongodb://localhost:27017/course_node');

// Import schemas
const enrollmentSchema = require('./models/enrollment.model');
const userSchema = require('./models/user.model');
const courseSchema = require('./models/course.model');

// Create models with their respective connections
const EnrollmentModel = enrollmentDB.model('Enrollment', enrollmentSchema);
const UserModel = authDB.model('User', userSchema);
const CourseModel = courseDB.model('Course', courseSchema);

// Make models available globally
app.set('models', {
  Enrollment: EnrollmentModel,
  User: UserModel,
  Course: CourseModel
});

// Routes
app.use('/api/enrollment', enrollmentRoutes);

// Health check endpoint
app.get('/health', (req, res) => {
  res.status(200).json({ status: 'OK', service: 'enrollment-service' });
});

// Handle connection events
enrollmentDB.on('connected', () => console.log('Connected to enrollment database'));
authDB.on('connected', () => console.log('Connected to auth database'));
courseDB.on('connected', () => console.log('Connected to course database'));

enrollmentDB.on('error', (err) => console.error('Enrollment database connection error:', err));
authDB.on('error', (err) => console.error('Auth database connection error:', err));
courseDB.on('error', (err) => console.error('Course database connection error:', err));

// Start server only after all connections are ready
Promise.all([
  new Promise(resolve => enrollmentDB.once('connected', resolve)),
  new Promise(resolve => authDB.once('connected', resolve)),
  new Promise(resolve => courseDB.once('connected', resolve))
])
.then(() => {
  app.listen(PORT, () => {
    console.log(`Enrollment service running on port ${PORT}`);
  });
})
.catch((error) => {
  console.error('Database connection error:', error);
  process.exit(1);
});

// Error handling middleware
app.use((err, req, res, next) => {
  console.error(err.stack);
  res.status(500).json({ error: 'Something went wrong!' });
}); 