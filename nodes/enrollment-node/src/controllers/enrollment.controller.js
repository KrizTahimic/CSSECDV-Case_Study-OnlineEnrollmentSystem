const { validationResult } = require('express-validator');
const axios = require('axios');

// Helper function to get models
const getModels = (req) => {
  const models = req.app.get('models');
  if (!models) {
    throw new Error('Models not found in app context');
  }
  return models;
};

const AUTH_SERVICE_URL = process.env.AUTH_SERVICE_URL || 'http://localhost:3001';
const COURSE_SERVICE_URL = process.env.COURSE_SERVICE_URL || 'http://localhost:3002';

// Enroll in a course
exports.enroll = async (req, res) => {
  try {
    const errors = validationResult(req);
    if (!errors.isEmpty()) {
      console.log('Validation errors:', errors.array());
      return res.status(400).json({ errors: errors.array() });
    }

    if (!req.user || !req.user.userId) {
      console.log('No user ID found in token:', req.user);
      return res.status(400).json({ message: 'User ID not found in token' });
    }

    const courseId = req.body.course;
    if (!courseId) {
      console.log('No course ID provided in request body');
      return res.status(400).json({ message: 'Course ID is required' });
    }

    const { Enrollment, Course, User } = getModels(req);

    // Verify course exists
    const course = await Course.findById(courseId);
    if (!course) {
      return res.status(404).json({ message: 'Course not found' });
    }

    console.log('Attempting to enroll:', {
      studentId: req.user.userId,
      courseId: courseId,
      courseName: course.title
    });

    // Check if student has any enrollment for this course
    const existingEnrollment = await Enrollment.findOne({
      student: req.user.userId,
      course: courseId
    });

    // If there's an existing enrollment and it's not dropped, return error
    if (existingEnrollment && existingEnrollment.status !== 'dropped') {
      console.log('Student already enrolled:', existingEnrollment);
      return res.status(400).json({ message: 'Already enrolled in this course' });
    }

    // If there's a dropped enrollment, update it instead of creating new
    if (existingEnrollment && existingEnrollment.status === 'dropped') {
      existingEnrollment.status = 'enrolled';
      existingEnrollment.enrollmentDate = new Date();
      await existingEnrollment.save();

      const populatedEnrollment = await existingEnrollment.populate([
        { path: 'course', select: 'code title description credits instructor capacity enrolled status schedule', model: Course },
        { path: 'student', select: 'firstName lastName email', model: User }
      ]);

      console.log('Re-enrollment successful:', populatedEnrollment);
      return res.status(200).json(populatedEnrollment);
    }

    // Create new enrollment if no existing enrollment found
    const enrollment = new Enrollment({
      student: req.user.userId,
      course: courseId,
      status: 'enrolled'
    });

    await enrollment.save();
    
    // Populate the enrollment with course and student details
    const populatedEnrollment = await enrollment.populate([
      { path: 'course', select: 'code title description credits instructor capacity enrolled status schedule', model: Course },
      { path: 'student', select: 'firstName lastName email', model: User }
    ]);

    console.log('New enrollment successful:', populatedEnrollment);
    res.status(201).json(populatedEnrollment);
  } catch (error) {
    console.error('Enrollment error:', error);
    res.status(500).json({ message: error.message });
  }
};

// Get enrollments
exports.getEnrollments = async (req, res) => {
  try {
    if (!req.user || !req.user.userId) {
      return res.status(400).json({ message: 'User ID not found in token' });
    }

    const { Enrollment, Course, User } = getModels(req);

    const enrollments = await Enrollment.find({
      $or: [
        { student: req.user.userId },
        { course: { $in: req.user.courses } } // For faculty
      ]
    }).populate({
      path: 'course',
      select: 'code title description credits instructor capacity enrolled status schedule',
      model: Course
    }).populate({
      path: 'student',
      select: 'firstName lastName email',
      model: User
    });
    
    console.log('Retrieved enrollments:', enrollments.length);
    res.json(enrollments);
  } catch (error) {
    console.error('Get enrollments error:', error);
    res.status(500).json({ message: error.message });
  }
};

// Update enrollment status
exports.updateEnrollmentStatus = async (req, res) => {
  try {
    const errors = validationResult(req);
    if (!errors.isEmpty()) {
      return res.status(400).json({ errors: errors.array() });
    }

    const { Enrollment } = getModels(req);

    const enrollment = await Enrollment.findByIdAndUpdate(
      req.params.id,
      { $set: { status: req.body.status } },
      { new: true, runValidators: true }
    );

    if (!enrollment) {
      return res.status(404).json({ message: 'Enrollment not found' });
    }
    
    res.json(enrollment);
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
};

// Drop enrollment
exports.dropEnrollment = async (req, res) => {
  try {
    const { Enrollment, Course, User } = getModels(req);

    const enrollment = await Enrollment.findOneAndUpdate(
      {
        _id: req.params.id,
        student: req.user.userId // Only allow students to drop their own enrollments
      },
      { $set: { status: 'dropped' } },
      { 
        new: true,
        runValidators: true
      }
    ).populate([
      { path: 'course', select: 'code title description credits instructor capacity enrolled status schedule', model: Course },
      { path: 'student', select: 'firstName lastName email', model: User }
    ]);

    if (!enrollment) {
      return res.status(404).json({ message: 'Enrollment not found or not authorized' });
    }
    
    console.log('Enrollment dropped:', enrollment);
    res.json(enrollment);
  } catch (error) {
    console.error('Drop enrollment error:', error);
    res.status(500).json({ message: error.message });
  }
};

// Get enrollments by course ID
exports.getEnrollmentsByCourse = async (req, res) => {
  try {
    const courseId = req.params.courseId;
    console.log('Request user:', req.user);
    
    // If faculty, verify they teach this course
    if (req.user.role === 'faculty') {
      try {
        const courseResponse = await axios.get(`${COURSE_SERVICE_URL}/api/courses/${courseId}`, {
          headers: { Authorization: req.headers.authorization }
        });
        
        console.log('Course data:', courseResponse.data);
        const instructorId = courseResponse.data.instructor?.toString();
        const userId = req.user.userId?.toString();
        console.log('Comparing instructor:', instructorId, 'with user:', userId);
        
        if (instructorId !== userId) {
          console.log('Authorization failed - instructor mismatch');
          return res.status(403).json({ 
            message: 'Not authorized to view enrollments for this course',
            debug: { instructorId, userId, user: req.user }
          });
        }
      } catch (error) {
        console.error('Error fetching course:', error);
        return res.status(404).json({ message: 'Course not found' });
      }
    }

    const { Enrollment } = getModels(req);
    const enrollments = await Enrollment.find({ 
      course: courseId,
      status: 'enrolled' // Only get active enrollments
    });

    // Get student details for each enrollment
    const enrollmentsWithStudents = await Promise.all(
      enrollments.map(async (enrollment) => {
        try {
          const studentResponse = await axios.get(`${AUTH_SERVICE_URL}/api/auth/users/${enrollment.student}`, {
            headers: { Authorization: req.headers.authorization }
          });
          
          return {
            ...enrollment.toObject(),
            student: studentResponse.data
          };
        } catch (error) {
          console.error(`Error fetching student details for ID ${enrollment.student}:`, error.message);
          // Return enrollment with basic student info even if we can't fetch details
          return {
            ...enrollment.toObject(),
            student: {
              _id: enrollment.student,
              firstName: 'Unknown',
              lastName: 'Student',
              email: 'student.not.found@example.com',
              error: 'Student details not found'
            }
          };
        }
      })
    );

    res.json(enrollmentsWithStudents);
  } catch (error) {
    console.error('Error in getEnrollmentsByCourse:', error);
    res.status(500).json({ message: 'Server error' });
  }
};

// Check if a student is enrolled in a course
exports.checkEnrollment = async (req, res) => {
  try {
    const studentId = req.params.studentId;
    const courseId = req.params.courseId;
    const { Enrollment } = getModels(req);  // Get the Enrollment model

    const enrollment = await Enrollment.findOne({
      student: studentId,
      course: courseId,
      status: 'enrolled'
    });

    res.json({
      enrolled: !!enrollment,
      enrollmentId: enrollment?._id
    });
  } catch (error) {
    console.error('Error checking enrollment:', error);
    res.status(500).json({ message: 'Server error' });
  }
}; 