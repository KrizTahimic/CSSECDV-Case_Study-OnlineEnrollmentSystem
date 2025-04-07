import React, { useState, useEffect } from 'react';
import {
  Container,
  Typography,
  Box,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  CircularProgress,
  Button,
  Alert,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  MenuItem
} from '@mui/material';
import { useNavigate } from 'react-router-dom';
import API_BASE_URLS from '../config/api';

// Helper function to capitalize first letter
const capitalizeFirstLetter = (string) => {
  return string.charAt(0).toUpperCase() + string.slice(1).toLowerCase();
};

const Enrollments = () => {
  const [enrollments, setEnrollments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [openEnrollmentDialog, setOpenEnrollmentDialog] = useState(false);
  const [selectedCourse, setSelectedCourse] = useState('');
  const [courses, setCourses] = useState([]);
  const navigate = useNavigate();
  const user = JSON.parse(localStorage.getItem('user'));

  const fetchEnrollments = async () => {
    try {
      const token = localStorage.getItem('token');
      if (!token) {
        navigate('/login');
        return;
      }

      const response = await fetch(API_BASE_URLS.ENROLLMENT, {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });

      if (response.ok) {
        const data = await response.json();
        setEnrollments(data);
      } else {
        const errorData = await response.json();
        setError(errorData.message || 'Failed to fetch enrollments');
      }
    } catch (err) {
      setError('Network error. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  const fetchCourses = async () => {
    try {
      const token = localStorage.getItem('token');
      const response = await fetch(API_BASE_URLS.COURSE, {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });

      if (response.ok) {
        const data = await response.json();
        setCourses(data);
      }
    } catch (err) {
      console.error('Error fetching courses:', err);
    }
  };

  useEffect(() => {
    fetchEnrollments();
    fetchCourses();
  }, [navigate]);

  const handleEnroll = async () => {
    try {
      const token = localStorage.getItem('token');
      const response = await fetch(API_BASE_URLS.ENROLLMENT, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          courseId: selectedCourse
        })
      });

      if (response.ok) {
        setSuccess('Successfully enrolled in the course');
        setOpenEnrollmentDialog(false);
        fetchEnrollments();
        setSelectedCourse('');
      } else {
        const errorData = await response.json();
        setError(errorData.message || 'Failed to enroll in the course');
      }
    } catch (err) {
      setError('Network error. Please try again.');
    }
  };

  const handleDrop = async (enrollmentId) => {
    try {
      const token = localStorage.getItem('token');
      const response = await fetch(`${API_BASE_URLS.ENROLLMENT}/${enrollmentId}`, {
        method: 'DELETE',
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });

      if (response.ok) {
        setSuccess('Successfully dropped the course');
        fetchEnrollments();
      } else {
        const errorData = await response.json();
        setError(errorData.message || 'Failed to drop the course');
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
          My Enrollments
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
        {user.role === 'student' && (
          <Box sx={{ mb: 2 }}>
            <Button
              variant="contained"
              color="primary"
              onClick={() => setOpenEnrollmentDialog(true)}
            >
              Enroll in a Course
            </Button>
          </Box>
        )}
        <TableContainer component={Paper}>
          <Table>
            <TableHead>
              <TableRow>
                <TableCell>Course Code</TableCell>
                <TableCell>Course Title</TableCell>
                <TableCell>Instructor</TableCell>
                <TableCell>Status</TableCell>
                <TableCell>Actions</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {enrollments.map((enrollment) => (
                <TableRow key={enrollment._id}>
                  <TableCell>{enrollment.course?.code}</TableCell>
                  <TableCell>{enrollment.course?.title}</TableCell>
                  <TableCell>
                    {enrollment.course?.instructor?.firstName}{' '}
                    {enrollment.course?.instructor?.lastName}
                  </TableCell>
                  <TableCell>{enrollment.status}</TableCell>
                  <TableCell>
                    {user.role === 'student' && (
                      <Button
                        variant="outlined"
                        color="error"
                        onClick={() => handleDrop(enrollment._id)}
                      >
                        Drop
                      </Button>
                    )}
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      </Box>

      <Dialog open={openEnrollmentDialog} onClose={() => setOpenEnrollmentDialog(false)}>
        <DialogTitle>Enroll in a Course</DialogTitle>
        <DialogContent>
          <TextField
            select
            fullWidth
            label="Course"
            value={selectedCourse}
            onChange={(e) => setSelectedCourse(e.target.value)}
            margin="normal"
          >
            {courses.map((course) => (
              <MenuItem key={course._id} value={course._id}>
                {course.code} - {course.title}
              </MenuItem>
            ))}
          </TextField>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpenEnrollmentDialog(false)}>Cancel</Button>
          <Button onClick={handleEnroll} variant="contained" color="primary">
            Enroll
          </Button>
        </DialogActions>
      </Dialog>
    </Container>
  );
};

export default Enrollments; 