const mongoose = require('mongoose');

const enrollmentSchema = new mongoose.Schema({
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
  status: {
    type: String,
    enum: ['pending', 'approved', 'rejected', 'dropped'],
    default: 'pending'
  },
  semester: {
    type: String,
    required: true
  },
  enrollmentDate: {
    type: Date,
    default: Date.now
  },
  lastUpdated: {
    type: Date,
    default: Date.now
  },
  prerequisitesMet: {
    type: Boolean,
    default: false
  },
  waitlistPosition: {
    type: Number,
    default: null
  }
});

// Update the lastUpdated timestamp before saving
enrollmentSchema.pre('save', function(next) {
  this.lastUpdated = Date.now();
  next();
});

// Compound index to prevent duplicate enrollments
enrollmentSchema.index({ student: 1, course: 1, semester: 1 }, { unique: true });

const Enrollment = mongoose.model('Enrollment', enrollmentSchema);

module.exports = Enrollment; 