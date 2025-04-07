import React, { useState, useEffect } from 'react';
import {
  Container,
  Typography,
  Box,
  Grid,
  Paper,
  CircularProgress,
  Button,
  Alert,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  MenuItem,
  Card,
  CardContent,
  CardActions
} from '@mui/material';
import { useNavigate } from 'react-router-dom';
import API_BASE_URLS from '../config/api';

const Courses = () => {
  const [courses, setCourses] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [openCourseDialog, setOpenCourseDialog] = useState(false);
  const [formData, setFormData] = useState({
    code: '',
    title: '',
    description: '',
    credits: 3,
    capacity: 30,
    schedule: {
      days: [],
      startTime: '',
      endTime: '',
      room: ''
    },
    instructorId: ''
  });
  const [instructors, setInstructors] = useState([]);
  const [userEnrollments, setUserEnrollments] = useState([]);
  const navigate = useNavigate();
  const user = JSON.parse(localStorage.getItem('user') || '{}');

  const isEnrolled = (courseId) => {
    return userEnrollments.some(enrollment => 
      (enrollment.courseId === courseId || enrollment.course?._id === courseId || enrollment.course?.id === courseId) && 
      enrollment.status === 'enrolled'
    );
  };

  const fetchUserEnrollments = async () => {
    if (user.role !== 'student') return;
    
    try {
      const token = localStorage.getItem('token');
      if (!token) return;

      const response = await fetch(`${API_BASE_URLS.ENROLLMENT}`, {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });

      if (response.ok) {
        const data = await response.json();
        console.log('User enrollments:', data);
        setUserEnrollments(data);
      }
    } catch (err) {
      console.error('Error fetching user enrollments:', err);
    }
  };

  const fetchCourses = async () => {
    try {
      const token = localStorage.getItem('token');
      if (!token) {
        navigate('/login');
        return;
      }

      const response = await fetch(API_BASE_URLS.COURSE, {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });

      if (response.ok) {
        const data = await response.json();
        setCourses(data);
      } else {
        const errorData = await response.json();
        setError(errorData.message || 'Failed to fetch courses');
      }
    } catch (err) {
      setError('Network error. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  const fetchInstructors = async () => {
    try {
      const token = localStorage.getItem('token');
      const response = await fetch(`${API_BASE_URLS.AUTH}/users?role=Faculty`, {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });

      if (response.ok) {
        const data = await response.json();
        setInstructors(data);
      }
    } catch (err) {
      console.error('Error fetching instructors:', err);
    }
  };

  useEffect(() => {
    fetchCourses();
    fetchInstructors();
    if (user.role === 'student') {
      fetchUserEnrollments();
    }
  }, [navigate]);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: value
    }));
  };

  const handleScheduleChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      schedule: {
        ...prev.schedule,
        [name]: value
      }
    }));
  };

  const handleDaysChange = (e) => {
    const { value } = e.target;
    setFormData(prev => ({
      ...prev,
      schedule: {
        ...prev.schedule,
        days: Array.isArray(value) ? value : [value]
      }
    }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setSuccess('');

    try {
      const token = localStorage.getItem('token');
      const response = await fetch(API_BASE_URLS.COURSE, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(formData)
      });

      if (response.ok) {
        setSuccess('Course created successfully');
        setOpenCourseDialog(false);
        fetchCourses();
        setFormData({
          code: '',
          title: '',
          description: '',
          credits: 3,
          capacity: 30,
          schedule: {
            days: [],
            startTime: '',
            endTime: '',
            room: ''
          },
          instructorId: ''
        });
      } else {
        const errorData = await response.json();
        setError(errorData.message || 'Failed to create course');
      }
    } catch (err) {
      setError('Network error. Please try again.');
    }
  };

  const handleDelete = async (courseId) => {
    try {
      const token = localStorage.getItem('token');
      const response = await fetch(`${API_BASE_URLS.COURSE}/${courseId}`, {
        method: 'DELETE',
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });

      if (response.ok) {
        setSuccess('Course deleted successfully');
        fetchCourses();
      } else {
        const errorData = await response.json();
        setError(errorData.message || 'Failed to delete course');
      }
    } catch (err) {
      setError('Network error. Please try again.');
    }
  };

  const handleEnroll = async (courseId) => {
    try {
      const token = localStorage.getItem('token');
      const response = await fetch(`${API_BASE_URLS.ENROLLMENT}`, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          courseId: courseId
        })
      });

      if (response.ok) {
        setSuccess('Successfully enrolled in course');
        fetchCourses();
        fetchUserEnrollments();
      } else {
        const errorData = await response.json();
        setError(errorData.message || 'Failed to enroll in course');
      }
    } catch (err) {
      setError('Network error. Please try again.');
    }
  };

  if (loading) {
    return (
      <Container>
        <Box sx={{ display: 'flex', justifyContent: 'center', mt: 4 }}>
          <CircularProgress />
        </Box>
      </Container>
    );
  }

  return (
    <Container>
      <Box sx={{ mt: 4, mb: 4 }}>
        <Typography variant="h4" component="h1" gutterBottom>
          Available Courses
        </Typography>
        {error && (
          <Alert severity="error" sx={{ mb: 2 }}>
            {error}
          </Alert>
        )}
        {success && (
          <Alert severity="success" sx={{ mb: 2 }}>
            {success}
          </Alert>
        )}
        {user.role === 'admin' && (
          <Box sx={{ mb: 2 }}>
            <Button
              variant="contained"
              color="primary"
              onClick={() => setOpenCourseDialog(true)}
            >
              Add Course
            </Button>
          </Box>
        )}
        
        <Grid container spacing={3}>
          {courses.map((course) => (
            <Grid item xs={12} sm={6} md={4} key={course.id || course._id}>
              <Card sx={{ 
                height: '100%', 
                display: 'flex', 
                flexDirection: 'column', 
                overflow: 'visible',
                borderTop: '4px solid #2e7d32',
                borderRadius: '4px'
              }}>
                <CardContent sx={{ flexGrow: 1, pb: 1 }}>
                  <Typography variant="h6" component="div" gutterBottom>
                    {course.code} - {course.title}
                  </Typography>
                  <Typography variant="body2" gutterBottom>
                    <strong>Instructor:</strong> {course.instructor ? 
                      `${course.instructor.firstName} ${course.instructor.lastName}` : 
                      'Unknown Instructor'}
                  </Typography>
                  <Typography variant="body2" gutterBottom>
                    <strong>Credits:</strong> {course.credits}
                  </Typography>
                  <Typography variant="body2" gutterBottom>
                    <strong>Schedule:</strong> {course.schedule?.days?.join(', ')} {course.schedule?.startTime} - {course.schedule?.endTime}
                  </Typography>
                  <Typography variant="body2" gutterBottom>
                    <strong>Room:</strong> {course.schedule?.room}
                  </Typography>
                  <Typography variant="body2" gutterBottom>
                    <strong>Available Spots:</strong> {course.capacity - (course.enrolled || 0)}
                  </Typography>
                </CardContent>
                <CardActions>
                  {(user.role === 'student' || !user.role) && (
                    <Button
                      variant="contained"
                      color="primary"
                      fullWidth
                      sx={{ 
                        bgcolor: isEnrolled(course.id || course._id) ? '#9e9e9e' : '#2e7d32', 
                        '&:hover': { bgcolor: isEnrolled(course.id || course._id) ? '#9e9e9e' : '#1b5e20' },
                        borderRadius: 0,
                        py: 0.75,
                        textTransform: 'uppercase',
                        fontWeight: 'bold'
                      }}
                      onClick={() => handleEnroll(course.id || course._id)}
                      disabled={course.status === 'closed' || (course.capacity <= course.enrolled) || isEnrolled(course.id || course._id)}
                    >
                      {isEnrolled(course.id || course._id) ? 'ENROLLED' : 'ENROLL'}
                    </Button>
                  )}
                  {user.role === 'admin' && (
                    <Button
                      variant="outlined"
                      color="error"
                      fullWidth
                      onClick={() => handleDelete(course.id || course._id)}
                    >
                      DELETE
                    </Button>
                  )}
                </CardActions>
              </Card>
            </Grid>
          ))}
          {courses.length === 0 && (
            <Grid item xs={12}>
              <Paper sx={{ p: 3, textAlign: 'center' }}>
                <Typography variant="body1">
                  No courses available for enrollment.
                </Typography>
              </Paper>
            </Grid>
          )}
        </Grid>
      </Box>

      <Dialog open={openCourseDialog} onClose={() => setOpenCourseDialog(false)}>
        <DialogTitle>Add Course</DialogTitle>
        <DialogContent>
          <TextField
            fullWidth
            label="Course Code"
            name="code"
            value={formData.code}
            onChange={handleChange}
            margin="normal"
            required
          />
          <TextField
            fullWidth
            label="Title"
            name="title"
            value={formData.title}
            onChange={handleChange}
            margin="normal"
            required
          />
          <TextField
            fullWidth
            label="Description"
            name="description"
            value={formData.description}
            onChange={handleChange}
            margin="normal"
            multiline
            rows={3}
            required
          />
          <TextField
            fullWidth
            label="Credits"
            name="credits"
            type="number"
            value={formData.credits}
            onChange={handleChange}
            margin="normal"
            required
          />
          <TextField
            fullWidth
            label="Capacity"
            name="capacity"
            type="number"
            value={formData.capacity}
            onChange={handleChange}
            margin="normal"
            required
          />
          <TextField
            select
            fullWidth
            label="Days"
            name="days"
            value={formData.schedule.days}
            onChange={handleDaysChange}
            margin="normal"
            SelectProps={{
              multiple: true
            }}
            required
          >
            <MenuItem value="Monday">Monday</MenuItem>
            <MenuItem value="Tuesday">Tuesday</MenuItem>
            <MenuItem value="Wednesday">Wednesday</MenuItem>
            <MenuItem value="Thursday">Thursday</MenuItem>
            <MenuItem value="Friday">Friday</MenuItem>
          </TextField>
          <TextField
            fullWidth
            label="Start Time"
            name="startTime"
            value={formData.schedule.startTime}
            onChange={handleScheduleChange}
            margin="normal"
            required
            placeholder="9:00 AM"
          />
          <TextField
            fullWidth
            label="End Time"
            name="endTime"
            value={formData.schedule.endTime}
            onChange={handleScheduleChange}
            margin="normal"
            required
            placeholder="10:30 AM"
          />
          <TextField
            fullWidth
            label="Room"
            name="room"
            value={formData.schedule.room}
            onChange={handleScheduleChange}
            margin="normal"
            required
            placeholder="Room 101"
          />
          <TextField
            select
            fullWidth
            label="Instructor"
            name="instructorId"
            value={formData.instructorId}
            onChange={handleChange}
            margin="normal"
            required
          >
            {instructors.map((instructor) => (
              <MenuItem key={instructor.id} value={instructor.id}>
                {instructor.firstName} {instructor.lastName}
              </MenuItem>
            ))}
          </TextField>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpenCourseDialog(false)}>Cancel</Button>
          <Button onClick={handleSubmit} variant="contained" color="primary">
            Add Course
          </Button>
        </DialogActions>
      </Dialog>
    </Container>
  );
};

export default Courses; 