import React, { useState, useEffect } from 'react';
import {
  Container,
  Typography,
  Box,
  Paper,
  CircularProgress,
  Button,
  Alert,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow
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
  const user = JSON.parse(localStorage.getItem('user') || '{}');

  useEffect(() => {
    // Redirect faculty users to dashboard
    if (user.role === 'faculty') {
      navigate('/dashboard');
      return;
    }

    const token = localStorage.getItem('token');
    if (!token) {
      navigate('/login');
      return;
    }

    fetchEnrollments();
    fetchCourses();
  }, [navigate, user.role]);

  const fetchEnrollments = async () => {
    try {
      const token = localStorage.getItem('token');
      if (!token) {
        navigate('/login');
        return;
      }

      // Get user from localStorage and ensure it exists
      const userStr = localStorage.getItem('user');
      if (!userStr) {
        navigate('/login');
        return;
      }

      const user = JSON.parse(userStr);
      console.log('Current user:', user);

      // Check if user is logged in and has necessary data
      if (!user || (!user.id && !user.email)) {
        console.error('User information not found');
        setError('User information not found. Please log in again.');
        navigate('/login');
        return;
      }

      const response = await fetch(`${API_BASE_URLS.ENROLLMENT}`, {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });

      if (response.ok) {
        const data = await response.json();
        console.log('Fetched enrollments:', data);
        setEnrollments(data);
      } else {
        const errorData = await response.json();
        console.error('Error response:', errorData);
        setError(errorData.message || 'Failed to fetch enrollments');
      }
    } catch (err) {
      console.error('Fetch error:', err);
      if (err.message.includes('Failed to fetch')) {
        setError('Unable to connect to the enrollment service. Please check if the service is running.');
      } else {
        setError(err.message || 'An unexpected error occurred while fetching enrollments.');
      }
    } finally {
      setLoading(false);
    }
  };

  const fetchCourses = async () => {
    try {
      const token = localStorage.getItem('token');
      const response = await fetch(`${API_BASE_URLS.COURSE}/open`, {
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
          courseId: selectedCourse,
          studentId: user.email
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

  const handleDrop = async (courseId) => {
    try {
      const token = localStorage.getItem('token');
      // Get user ID from the token instead of local storage
      const tokenParts = token.split('.');
      const payload = JSON.parse(atob(tokenParts[1]));
      console.log('Token payload:', payload);
      
      const userId = payload.id;
      console.log('Using user ID from token:', userId);
      
      // Log courseId to verify it's correct
      console.log('Dropping course with ID:', courseId);
      
      if (!userId) {
        setError('User ID not found in token. Please log in again.');
        return;
      }
      
      const response = await fetch(`${API_BASE_URLS.ENROLLMENT}/student/${userId}/course/${courseId}`, {
        method: 'DELETE',
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });

      if (response.ok) {
        setSuccess('Successfully dropped the course');
        // Refresh enrollments to update the list
        fetchEnrollments();
        // Refresh courses to update availability
        fetchCourses();
      } else {
        const errorData = await response.json().catch(() => ({}));
        setError(errorData.message || 'Failed to drop the course');
      }
    } catch (err) {
      console.error('Drop error:', err);
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
          Enrollments
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
          <Box sx={{ mb: 3 }}>
            <Button
              variant="contained"
              sx={{ 
                bgcolor: '#2e7d32',
                '&:hover': { bgcolor: '#1b5e20' },
                textTransform: 'uppercase',
                fontWeight: 'bold'
              }}
              onClick={() => navigate('/courses')}
            >
              ENROLL IN A COURSE
            </Button>
          </Box>
        )}

        <TableContainer component={Paper}>
          <Table>
            <TableHead>
              <TableRow>
                <TableCell>Course</TableCell>
                <TableCell>Instructor</TableCell>
                <TableCell>Enrollment Date</TableCell>
                <TableCell>Status</TableCell>
                <TableCell>Actions</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {enrollments.length > 0 ? (
                enrollments.map((enrollment) => (
                  <TableRow key={enrollment.id}>
                    <TableCell>
                      {enrollment.course?.code} - {enrollment.course?.title}
                    </TableCell>
                    <TableCell>
                      {enrollment.course?.instructor ? 
                        `${enrollment.course.instructor.firstName} ${enrollment.course.instructor.lastName}` 
                        : 'Unknown Instructor'}
                    </TableCell>
                    <TableCell>
                      {new Date(enrollment.enrollmentDate).toLocaleDateString()}
                    </TableCell>
                    <TableCell>
                      {capitalizeFirstLetter(enrollment.status)}
                    </TableCell>
                    <TableCell>
                      {enrollment.status !== 'dropped' && (
                        <Button
                          variant="outlined"
                          color="error"
                          size="small"
                          onClick={() => {
                            console.log("Dropping enrollment:", enrollment);
                            const courseId = enrollment.courseId || enrollment.course?._id;
                            handleDrop(courseId);
                          }}
                        >
                          DROP
                        </Button>
                      )}
                    </TableCell>
                  </TableRow>
                ))
              ) : (
                <TableRow>
                  <TableCell colSpan={5} align="center">
                    You are not enrolled in any courses yet.
                  </TableCell>
                </TableRow>
              )}
            </TableBody>
          </Table>
        </TableContainer>
      </Box>
    </Container>
  );
};

export default Enrollments; 