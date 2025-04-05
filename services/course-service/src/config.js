require('dotenv').config();

module.exports = {
  port: process.env.PORT || 3002,
  mongoUri: process.env.MONGODB_URI || 'mongodb://localhost:27017/enrollment_courses',
  jwtSecret: process.env.JWT_SECRET || 'your_jwt_secret_key'
}; 