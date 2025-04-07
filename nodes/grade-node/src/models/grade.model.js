const mongoose = require('mongoose');

const gradeSchema = new mongoose.Schema({
  student: {
    type: mongoose.Schema.Types.ObjectId,
    ref: 'User',
    required: true
  },
  course: {
    type: mongoose.Schema.Types.ObjectId,
    ref: 'Course',
    required: true
  },
  grade: {
    type: Number,
    min: 0,
    max: 4.0,
    required: true
  },
  score: {
    type: Number,
    min: 0,
    max: 100,
    required: true
  },
  comments: {
    type: String,
    trim: true
  },
  submittedBy: {
    type: mongoose.Schema.Types.ObjectId,
    ref: 'User',
    required: true
  },
  submittedAt: {
    type: Date,
    default: Date.now
  },
  lastUpdated: {
    type: Date,
    default: Date.now
  }
});

// Update the lastUpdated timestamp before saving
gradeSchema.pre('save', function(next) {
  this.lastUpdated = Date.now();
  next();
});

// Compound index to prevent duplicate grades for the same student in the same course
gradeSchema.index({ student: 1, course: 1 }, { unique: true });

module.exports = mongoose.model('Grade', gradeSchema); 