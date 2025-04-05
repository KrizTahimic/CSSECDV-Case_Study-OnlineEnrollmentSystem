const Course = require('../models/course.model');
const { validationResult } = require('express-validator');

exports.createCourse = async (req, res) => {
  try {
    const errors = validationResult(req);
    if (!errors.isEmpty()) {
      return res.status(400).json({ errors: errors.array() });
    }

    const course = new Course({
      ...req.body,
      instructor: req.user.userId
    });

    await course.save();
    res.status(201).json(course);
  } catch (error) {
    console.error('Course creation error:', error);
    res.status(500).json({ message: 'Server error' });
  }
};

exports.getCourses = async (req, res) => {
  try {
    const { semester, status, instructor } = req.query;
    const query = {};

    if (semester) query.semester = semester;
    if (status) query.status = status;
    if (instructor) query.instructor = instructor;

    const courses = await Course.find(query)
      .populate('instructor', 'firstName lastName email')
      .sort({ createdAt: -1 });

    res.json(courses);
  } catch (error) {
    console.error('Course fetch error:', error);
    res.status(500).json({ message: 'Server error' });
  }
};

exports.getCourseById = async (req, res) => {
  try {
    const course = await Course.findById(req.params.id)
      .populate('instructor', 'firstName lastName email');

    if (!course) {
      return res.status(404).json({ message: 'Course not found' });
    }

    res.json(course);
  } catch (error) {
    console.error('Course fetch error:', error);
    res.status(500).json({ message: 'Server error' });
  }
};

exports.updateCourse = async (req, res) => {
  try {
    const errors = validationResult(req);
    if (!errors.isEmpty()) {
      return res.status(400).json({ errors: errors.array() });
    }

    const course = await Course.findById(req.params.id);

    if (!course) {
      return res.status(404).json({ message: 'Course not found' });
    }

    // Only instructor or admin can update the course
    if (course.instructor.toString() !== req.user.userId && req.user.role !== 'admin') {
      return res.status(403).json({ message: 'Not authorized to update this course' });
    }

    Object.assign(course, req.body);
    await course.save();

    res.json(course);
  } catch (error) {
    console.error('Course update error:', error);
    res.status(500).json({ message: 'Server error' });
  }
};

exports.deleteCourse = async (req, res) => {
  try {
    const course = await Course.findById(req.params.id);

    if (!course) {
      return res.status(404).json({ message: 'Course not found' });
    }

    // Only instructor or admin can delete the course
    if (course.instructor.toString() !== req.user.userId && req.user.role !== 'admin') {
      return res.status(403).json({ message: 'Not authorized to delete this course' });
    }

    await course.remove();
    res.json({ message: 'Course deleted successfully' });
  } catch (error) {
    console.error('Course deletion error:', error);
    res.status(500).json({ message: 'Server error' });
  }
};

exports.updateCourseStatus = async (req, res) => {
  try {
    const { status } = req.body;
    const course = await Course.findById(req.params.id);

    if (!course) {
      return res.status(404).json({ message: 'Course not found' });
    }

    // Only instructor or admin can update the course status
    if (course.instructor.toString() !== req.user.userId && req.user.role !== 'admin') {
      return res.status(403).json({ message: 'Not authorized to update this course' });
    }

    course.status = status;
    await course.save();

    res.json(course);
  } catch (error) {
    console.error('Course status update error:', error);
    res.status(500).json({ message: 'Server error' });
  }
}; 