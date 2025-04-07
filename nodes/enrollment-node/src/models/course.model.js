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
  status: {
    type: String,
    enum: ['open', 'closed', 'cancelled'],
    default: 'open'
  },
  schedule: {
    days: [{
      type: String,
      enum: ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday']
    }],
    startTime: String,
    endTime: String,
    room: String
  }
});

module.exports = courseSchema; 