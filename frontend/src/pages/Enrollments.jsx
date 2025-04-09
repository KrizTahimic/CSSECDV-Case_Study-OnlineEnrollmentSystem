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
  const [enrollmentLoading, setEnrollmentLoading] = useState(false);
  const [showSuccess, setShowSuccess] = useState(false);
  const [successMessage, setSuccessMessage] = useState('');

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

  const handleEnroll = async (courseId) => {
    try {
      setEnrollmentLoading(true);
      const token = localStorage.getItem('token');
      
      const response = await fetch(`${API_BASE_URLS.ENROLLMENT}`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify({ courseId })
      });

      if (response.ok) {
        const result = await response.json();
        console.log('Enrollment successful:', result);
        
        // Show success message
        setSuccessMessage('Successfully enrolled in the course!');
        setShowSuccess(true);
        
        // Hide success message after 5 seconds
        setTimeout(() => {
          setShowSuccess(false);
        }, 5000);
        
        // Refresh enrollments after successful enrollment
        fetchEnrollments();
      } else {
        const errorData = await response.json();
        console.error('Enrollment failed:', errorData);
        setError(errorData.error || errorData.message || 'Failed to enroll in the course. Please try again.');
      }
    } catch (err) {
      console.error('Enrollment error:', err);
      if (err.message.includes('Failed to fetch') || err.name === 'TypeError') {
        setError('Unable to connect to the enrollment service. The service might be down or experiencing issues. Please try again later.');
      } else {
        setError('An unexpected error occurred during enrollment: ' + err.message);
      }
    } finally {
      setEnrollmentLoading(false);
    }
  };

  const handleUnenroll = async (courseId) => {
    try {
      setEnrollmentLoading(true);
      const token = localStorage.getItem('token');
      const userId = user.id;
      
      if (!userId) {
        setError('User ID not found. Please log in again.');
        navigate('/login');
        return;
      }
      
      const response = await fetch(`${API_BASE_URLS.ENROLLMENT}/student/${userId}/course/${courseId}`, {
        method: 'DELETE',
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });

      if (response.ok) {
        console.log('Unenrollment successful');
        
        // Show success message
        setSuccessMessage('Successfully dropped the course!');
        setShowSuccess(true);
        
        // Hide success message after 5 seconds
        setTimeout(() => {
          setShowSuccess(false);
        }, 5000);
        
        // Refresh enrollments after successful unenrollment
        fetchEnrollments();
      } else {
        try {
          const errorData = await response.json();
          console.error('Unenrollment failed:', errorData);
          setError(errorData.error || errorData.message || 'Failed to drop the course. Please try again.');
        } catch (e) {
          // If the response is not JSON
          console.error('Failed to parse error response:', e);
          setError('Failed to drop the course. Please try again.');
        }
      }
    } catch (err) {
      console.error('Unenrollment error:', err);
      if (err.message.includes('Failed to fetch') || err.name === 'TypeError') {
        setError('Unable to connect to the enrollment service. The service might be down or experiencing issues. Please try again later.');
      } else {
        setError('An unexpected error occurred during course drop: ' + err.message);
      }
    } finally {
      setEnrollmentLoading(false);
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
                            handleUnenroll(courseId);
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