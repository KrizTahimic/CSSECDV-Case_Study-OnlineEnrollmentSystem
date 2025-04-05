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
  semester: {
    type: String,
    required: true
  },
  grade: {
    type: String,
    enum: ['A', 'A-', 'B+', 'B', 'B-', 'C+', 'C', 'C-', 'D+', 'D', 'F', 'I', 'W'],
    required: true
  },
  score: {
    type: Number,
    min: 0,
    max: 100
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

// Compound index to prevent duplicate grades
gradeSchema.index({ student: 1, course: 1, semester: 1 }, { unique: true });

const Grade = mongoose.model('Grade', gradeSchema);

module.exports = Grade; 