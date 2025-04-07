const Course = require('../models/course.model');
const { validationResult } = require('express-validator');

// Create a new course
exports.createCourse = async (req, res) => {
  try {
    const errors = validationResult(req);
    if (!errors.isEmpty()) {
      return res.status(400).json({ errors: errors.array() });
    }

    const course = new Course({
      code: req.body.code,
      title: req.body.title,
      description: req.body.description,
      credits: req.body.credits,
      instructor: req.body.instructor,
      capacity: req.body.capacity,
      schedule: req.body.schedule,
      prerequisites: req.body.prerequisites
    });

    await course.save();
    res.status(201).json(course);
  } catch (error) {
    if (error.code === 11000) {
      res.status(400).json({ message: 'Course code already exists' });
    } else {
      console.error('Create course error:', error);
      res.status(500).json({ message: 'Server error' });
    }
  }
};

// Get all courses
exports.getCourses = async (req, res) => {
  try {
    console.log('Fetching all courses...');
    const courses = await Course.find();
    console.log(`Found ${courses.length} courses`);
    res.json(courses);
  } catch (error) {
    console.error('Error fetching courses:', error);
    res.status(500).json({ 
      message: 'Error fetching courses',
      error: error.message 
    });
  }
};

// Get a single course
exports.getCourse = async (req, res) => {
  try {
    console.log(`Fetching course with ID: ${req.params.id}`);
    const course = await Course.findById(req.params.id);
    
    if (!course) {
      console.log('Course not found');
      return res.status(404).json({ message: 'Course not found' });
    }
    
    console.log('Course found successfully');
    res.json(course);
  } catch (error) {
    console.error('Error fetching course:', error);
    res.status(500).json({ 
      message: 'Error fetching course',
      error: error.message 
    });
  }
};

// Update a course
exports.updateCourse = async (req, res) => {
  try {
    const errors = validationResult(req);
    if (!errors.isEmpty()) {
      return res.status(400).json({ errors: errors.array() });
    }

    const course = await Course.findByIdAndUpdate(
      req.params.id,
      { $set: req.body },
      { new: true, runValidators: true }
    );

    if (!course) {
      return res.status(404).json({ message: 'Course not found' });
    }
    
    res.json(course);
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
};

// Delete a course
exports.deleteCourse = async (req, res) => {
  try {
    const result = await Course.deleteOne({ _id: req.params.id });
    if (result.deletedCount === 0) {
      return res.status(404).json({ message: 'Course not found' });
    }
    res.json({ message: 'Course deleted' });
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
};

// Update course status
exports.updateCourseStatus = async (req, res) => {
  try {
    const { status } = req.body;
    const course = await Course.findByIdAndUpdate(
      req.params.id,
      { $set: { status } },
      { new: true, runValidators: true }
    );

    if (!course) {
      return res.status(404).json({ message: 'Course not found' });
    }

    res.json(course);
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
}; 