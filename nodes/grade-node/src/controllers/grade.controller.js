const Grade = require('../models/grade.model');
const axios = require('axios');
const { validationResult } = require('express-validator');

const ENROLLMENT_SERVICE_URL = process.env.ENROLLMENT_SERVICE_URL || 'http://localhost:3003';
const COURSE_SERVICE_URL = process.env.COURSE_SERVICE_URL || 'http://localhost:3002';
const AUTH_SERVICE_URL = process.env.AUTH_SERVICE_URL || 'http://localhost:3001';

// Submit a grade
exports.submitGrade = async (req, res) => {
  try {
    const errors = validationResult(req);
    if (!errors.isEmpty()) {
      return res.status(400).json({ errors: errors.array() });
    }

    // Verify the faculty is teaching the course
    const courseResponse = await axios.get(`${COURSE_SERVICE_URL}/api/courses/${req.body.courseId}`, {
      headers: { Authorization: req.headers.authorization }
    });

    console.log('Course data:', courseResponse.data);
    console.log('User data:', req.user);

    if (courseResponse.data.instructor !== req.user.userId) {
      return res.status(403).json({ 
        message: 'Not authorized to submit grades for this course',
        debug: {
          courseInstructor: courseResponse.data.instructor,
          userId: req.user.userId
        }
      });
    }

    // Verify student is enrolled in the course
    const enrollmentResponse = await axios.get(
      `${ENROLLMENT_SERVICE_URL}/api/enrollment/check/${req.body.studentId}/${req.body.courseId}`,
      { headers: { Authorization: req.headers.authorization } }
    );

    if (!enrollmentResponse.data.enrolled) {
      return res.status(400).json({ message: 'Student is not enrolled in this course' });
    }

    // Check if a grade already exists
    const existingGrade = await Grade.findOne({
      student: req.body.studentId,
      course: req.body.courseId
    });

    if (existingGrade) {
      // Update existing grade
      existingGrade.grade = req.body.grade;
      existingGrade.score = req.body.score;
      existingGrade.comments = req.body.comments;
      existingGrade.submittedBy = req.user.userId;
      existingGrade.lastUpdated = Date.now();
      
      await existingGrade.save();
      return res.json(existingGrade);
    }

    // Create new grade
    const grade = new Grade({
      student: req.body.studentId,
      course: req.body.courseId,
      grade: req.body.grade,
      score: req.body.score,
      comments: req.body.comments,
      submittedBy: req.user.userId
    });

    await grade.save();
    res.status(201).json(grade);
  } catch (error) {
    console.error('Grade submission error:', error);
    res.status(500).json({ message: error.response?.data?.message || error.message });
  }
};

// Get grades with role-based access control
exports.getGrades = async (req, res) => {
  try {
    let query = {};
    console.log('User data in getGrades:', req.user);
    
    // Students can only see their own grades
    if (req.user.role === 'student') {
      query.student = req.user.userId;
    }
    // Faculty can only see grades for their courses
    else if (req.user.role === 'faculty') {
      // Get faculty's courses
      const coursesResponse = await axios.get(`${COURSE_SERVICE_URL}/api/courses`, {
        headers: { Authorization: req.headers.authorization }
      });
      
      console.log('Faculty courses:', coursesResponse.data);
      const facultyCourses = coursesResponse.data
        .filter(course => course.instructor === req.user.userId)
        .map(course => course._id);
      
      console.log('Filtered faculty courses:', facultyCourses);
      query.course = { $in: facultyCourses };
    }

    console.log('Final query:', query);
    const grades = await Grade.find(query)
      .sort({ lastUpdated: -1 });

    console.log('Found grades:', grades);

    // Fetch user and course details from respective services
    const populatedGrades = await Promise.all(grades.map(async (grade) => {
      try {
        // Fetch student details
        const studentResponse = await axios.get(`${AUTH_SERVICE_URL}/api/auth/users/${grade.student}`, {
          headers: { Authorization: req.headers.authorization }
        });

        // Fetch course details
        const courseResponse = await axios.get(`${COURSE_SERVICE_URL}/api/courses/${grade.course}`, {
          headers: { Authorization: req.headers.authorization }
        });

        // Fetch submitter details
        const submitterResponse = await axios.get(`${AUTH_SERVICE_URL}/api/auth/users/${grade.submittedBy}`, {
          headers: { Authorization: req.headers.authorization }
        });

        return {
          ...grade.toObject(),
          student: studentResponse.data,
          course: courseResponse.data,
          submittedBy: submitterResponse.data
        };
      } catch (error) {
        console.error('Error populating grade details:', error);
        return grade;
      }
    }));

    console.log('Populated grades:', populatedGrades);
    res.json(populatedGrades);
  } catch (error) {
    console.error('Get grades error:', error);
    res.status(500).json({ message: error.response?.data?.message || error.message });
  }
};

// Get grades for a specific student
exports.getStudentGrades = async (req, res) => {
  try {
    const studentId = req.params.studentId;

    // Only allow students to view their own grades or faculty to view their students' grades
    if (req.user.role === 'student' && req.user.id !== studentId) {
      return res.status(403).json({ message: 'Not authorized to view these grades' });
    }

    if (req.user.role === 'faculty') {
      // Verify the student is enrolled in one of the faculty's courses
      const coursesResponse = await axios.get(`${COURSE_SERVICE_URL}/api/courses`, {
        headers: { Authorization: req.headers.authorization }
      });
      
      const facultyCourses = coursesResponse.data
        .filter(course => course.instructor === req.user.id)
        .map(course => course._id);

      const grades = await Grade.find({
        student: studentId,
        course: { $in: facultyCourses }
      })
        .populate({
          path: 'course',
          select: 'code title credits',
          model: 'Course'
        })
        .sort({ lastUpdated: -1 });

      return res.json(grades);
    }

    // For students viewing their own grades
    const grades = await Grade.find({ student: studentId })
      .populate({
        path: 'course',
        select: 'code title credits',
        model: 'Course'
      })
      .sort({ lastUpdated: -1 });

    res.json(grades);
  } catch (error) {
    console.error('Get student grades error:', error);
    res.status(500).json({ message: error.response?.data?.message || error.message });
  }
};

// Update a grade
exports.updateGrade = async (req, res) => {
  try {
    const errors = validationResult(req);
    if (!errors.isEmpty()) {
      return res.status(400).json({ errors: errors.array() });
    }

    const grade = await Grade.findById(req.params.id);
    if (!grade) {
      return res.status(404).json({ message: 'Grade not found' });
    }

    // Verify the faculty is teaching the course
    const courseResponse = await axios.get(`${COURSE_SERVICE_URL}/api/courses/${grade.course}`, {
      headers: { Authorization: req.headers.authorization }
    });

    if (courseResponse.data.instructor !== req.user.id) {
      return res.status(403).json({ message: 'Not authorized to update grades for this course' });
    }

    grade.grade = req.body.grade;
    grade.score = req.body.score;
    grade.comments = req.body.comments;
    grade.submittedBy = req.user.id;
    grade.lastUpdated = Date.now();

    await grade.save();
    res.json(grade);
  } catch (error) {
    console.error('Update grade error:', error);
    res.status(500).json({ message: error.response?.data?.message || error.message });
  }
};

// Delete a grade (restricted to original submitter)
exports.deleteGrade = async (req, res) => {
  try {
    const grade = await Grade.findById(req.params.id);
    if (!grade) {
      return res.status(404).json({ message: 'Grade not found' });
    }

    // Only allow the original submitter to delete the grade
    if (grade.submittedBy.toString() !== req.user.id) {
      return res.status(403).json({ message: 'Not authorized to delete this grade' });
    }

    await grade.deleteOne();
    res.json({ message: 'Grade deleted successfully' });
  } catch (error) {
    console.error('Delete grade error:', error);
    res.status(500).json({ message: error.response?.data?.message || error.message });
  }
}; 