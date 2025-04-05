const mongoose = require('mongoose');

const courseSchema = new mongoose.Schema({
  code: {
    type: String,
    required: true,
    unique: true,
    trim: true
  },
  title: {
    type: String,
    required: true,
    trim: true
  },
  description: {
    type: String,
    required: true
  },
  credits: {
    type: Number,
    required: true,
    min: 1
  },
  instructor: {
    type: mongoose.Schema.Types.ObjectId,
    ref: 'User',
    required: true
  },
  capacity: {
    type: Number,
    required: true,
    min: 1
  },
  enrolled: {
    type: Number,
    default: 0
  },
  semester: {
    type: String,
    required: true
  },
  status: {
    type: String,
    enum: ['open', 'closed', 'cancelled'],
    default: 'open'
  },
  prerequisites: [{
    type: String,
    trim: true
  }],
  schedule: {
    days: [{
      type: String,
      enum: ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday']
    }],
    startTime: String,
    endTime: String,
    room: String
  },
  createdAt: {
    type: Date,
    default: Date.now
  },
  updatedAt: {
    type: Date,
    default: Date.now
  }
});

// Update the updatedAt timestamp before saving
courseSchema.pre('save', function(next) {
  this.updatedAt = Date.now();
  next();
});

const Course = mongoose.model('Course', courseSchema);

module.exports = Course; 