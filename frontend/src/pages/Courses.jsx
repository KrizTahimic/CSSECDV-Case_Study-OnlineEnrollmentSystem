import React, { useState, useEffect } from 'react';
import { Container, Typography, Box, Grid, Card, CardContent, CardActions, Button, CircularProgress, Snackbar, Alert } from '@mui/material';
import { useNavigate } from 'react-router-dom';

const Courses = () => {
  const [courses, setCourses] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [instructors, setInstructors] = useState({});
  const [user, setUser] = useState(null);
  const [enrollments, setEnrollments] = useState([]);
  const [successMessage, setSuccessMessage] = useState('');
  const navigate = useNavigate();

  const fetchInstructorDetails = async (instructorId) => {
    try {
      const token = localStorage.getItem('token');
      if (!token) {
        console.warn('No token found for instructor fetch');
        return;
      }
      
      console.log('Fetching instructor details for ID:', instructorId);
      
      const response = await fetch(`http://localhost:3001/api/auth/users/${instructorId}`, {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });
      
      if (!response.ok) {
        console.warn(`Failed to fetch instructor details for ID: ${instructorId}. Status: ${response.status}`);
        return;
      }
      
      const data = await response.json();
      console.log('Received instructor data:', data);
      
      if (!data || !data.firstName || !data.lastName) {
        console.warn('Invalid instructor data received:', data);
        return;
      }
      
      setInstructors(prev => {
        if (prev[instructorId]?.firstName === data.firstName && 
            prev[instructorId]?.lastName === data.lastName) {
          return prev;
        }
        return {
          ...prev,
          [instructorId]: {
            firstName: data.firstName,
            lastName: data.lastName
          }
        };
      });
    } catch (error) {
      console.error('Error fetching instructor details:', error);
    }
  };

  const fetchUserData = async () => {
    try {
      const token = localStorage.getItem('token');
      const userData = JSON.parse(localStorage.getItem('user'));
      
      if (!token) {
        navigate('/login');
        return;
      }
      
      setUser(userData);
      await fetchCourses(userData);
    } catch (err) {
      console.error('Error in fetchUserData:', err);
      setError('Network error. Please try again.');
    }
  };

  const fetchCourses = async (currentUser) => {
    try {
      setLoading(true);
      const token = localStorage.getItem('token');
      if (!token) {
        navigate('/login');
        return;
      }

      const response = await fetch('http://localhost:3002/api/courses', {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });

      if (!response.ok) {
        throw new Error('Failed to fetch courses');
      }

      const data = await response.json();
      console.log('Fetched courses:', data);
      
      // Filter courses based on user role using the passed currentUser
      if (currentUser && currentUser.role === 'faculty') {
        console.log('Current user:', currentUser);
        
        const teacherCourses = data.filter(course => {
          console.log(`Comparing course ${course.code}:`, {
            courseInstructor: course.instructor,
            userId: currentUser.id
          });
          
          return course.instructor && course.instructor.toString() === currentUser.id.toString();
        });
        
        console.log('Teacher courses:', teacherCourses);
        setCourses(teacherCourses);
      } else {
        setCourses(data);
      }
      
    } catch (error) {
      console.error('Error in fetchCourses:', error);
      setError(error.message);
    } finally {
      setLoading(false);
    }
  };

  const fetchEnrollments = async () => {
    try {
      const token = localStorage.getItem('token');
      if (!token) return;

      const response = await fetch('http://localhost:3003/api/enrollment', {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });

      if (response.ok) {
        const data = await response.json();
        setEnrollments(data);
      }
    } catch (error) {
      console.error('Error fetching enrollments:', error);
    }
  };

  useEffect(() => {
    fetchUserData();
    fetchEnrollments();
  }, [navigate]);

  // Add a useEffect hook to fetch instructor details when the component mounts
  useEffect(() => {
    if (courses.length > 0) {
      console.log('Fetching instructor details for all courses');
      courses.forEach(course => {
        if (course.instructor && !instructors[course.instructor]) {
          console.log('Fetching details for instructor:', course.instructor);
          fetchInstructorDetails(course.instructor);
        }
      });
    }
  }, [courses, instructors]);

  // Helper function to check if user is enrolled in a course
  const isEnrolled = (courseId) => {
    return enrollments.some(enrollment => 
      enrollment.course._id === courseId && enrollment.status === 'enrolled'
    );
  };

  const handleCloseSnackbar = () => {
    setSuccessMessage('');
  };

  const handleEnroll = async (courseId) => {
    try {
      const token = localStorage.getItem('token');
      if (!token) {
        navigate('/login');
        return;
      }

      if (!user) {
        setError('User data not found. Please log in again.');
        return;
      }

      if (user.role !== 'student') {
        setError('Only students can enroll in courses');
        return;
      }

      const response = await fetch('http://localhost:3003/api/enrollment', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify({
          course: courseId
        })
      });

      if (response.ok) {
        const data = await response.json();
        const action = data.status === 'enrolled' ? 'enrolled in' : 're-enrolled in';
        setSuccessMessage(`Successfully ${action} the course!`);
        await fetchEnrollments();
        await fetchCourses(user);
        setError('');
        return;
      }

      try {
        const errorData = await response.json();
        setError(errorData.message || 'Failed to enroll in course');
      } catch (parseError) {
        setError(`Failed to enroll in course: ${response.statusText}`);
      }
    } catch (err) {
      console.error('Error in handleEnroll:', err);
      setError('Network error. Please try again.');
    }
  };

  // Add this helper function to format the schedule
  const formatSchedule = (schedule) => {
    if (!schedule) return 'Not scheduled';
    
    // If schedule is a string, return it directly
    if (typeof schedule === 'string') return schedule;
    
    // If schedule is an object with days, startTime, endTime, room
    if (schedule.days && schedule.startTime && schedule.endTime) {
      return `${schedule.days.join(', ')} ${schedule.startTime}-${schedule.endTime}`;
    }
    
    // Fallback
    return JSON.stringify(schedule);
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

  if (error) {
    return (
      <Container>
        <Box sx={{ mt: 4 }}>
          <Typography color="error" variant="h6">
            {error}
          </Typography>
        </Box>
      </Container>
    );
  }

  return (
    <Container maxWidth="lg">
      <Box sx={{ mt: 4, mb: 4 }}>
        <Typography variant="h4" component="h1" gutterBottom>
          {user?.role === 'faculty' ? 'My Courses' : 'Available Courses'}
        </Typography>
        {error && (
          <Alert severity="error" sx={{ mb: 2 }}>
            {error}
          </Alert>
        )}
        <Grid container spacing={3}>
          {courses.length === 0 ? (
            <Grid item xs={12}>
              <Alert severity="info">
                {user?.role === 'faculty' 
                  ? 'You are not assigned to teach any courses yet.'
                  : 'No courses are available at the moment.'}
              </Alert>
            </Grid>
          ) : (
            courses.map((course) => (
              <Grid item xs={12} sm={6} md={4} key={course._id}>
                <Card sx={{ 
                  height: '100%', 
                  display: 'flex', 
                  flexDirection: 'column',
                  position: 'relative'
                }}>
                  <CardContent sx={{ 
                    flexGrow: 1,
                    pb: user?.role === 'student' ? 0 : 2
                  }}>
                    <Typography variant="h6" gutterBottom>
                      {course.code} - {course.title}
                    </Typography>
                    {user?.role !== 'faculty' && (
                      <Typography color="textSecondary" gutterBottom>
                        Instructor: {instructors[course.instructor] ? 
                          `${instructors[course.instructor].firstName} ${instructors[course.instructor].lastName}` : 
                          'Loading...'}
                      </Typography>
                    )}
                    <Box sx={{ mt: 2 }}>
                      <Typography variant="body2" gutterBottom>
                        Credits: {course.credits}
                      </Typography>
                      <Typography variant="body2" gutterBottom>
                        Schedule: {formatSchedule(course.schedule)}
                      </Typography>
                      <Typography variant="body2" gutterBottom>
                        Room: {course.schedule?.room || 'TBD'}
                      </Typography>
                      <Typography variant="body2" gutterBottom>
                        Available Spots: {course.capacity - (course.enrolled || 0)}
                      </Typography>
                    </Box>
                  </CardContent>
                  {user && user.role === 'student' && (
                    <CardActions sx={{ 
                      p: 2,
                      pt: 0,
                      mt: 'auto'
                    }}>
                      <Button 
                        size="small" 
                        color="primary"
                        variant={isEnrolled(course._id) ? "outlined" : "contained"}
                        onClick={() => handleEnroll(course._id)}
                        disabled={
                          (course.enrolled || 0) >= course.capacity || 
                          isEnrolled(course._id)
                        }
                        fullWidth
                      >
                        {isEnrolled(course._id) ? 'Enrolled' : 'Enroll'}
                      </Button>
                    </CardActions>
                  )}
                </Card>
              </Grid>
            ))
          )}
        </Grid>
      </Box>
      <Snackbar 
        open={!!successMessage} 
        autoHideDuration={4000} 
        onClose={handleCloseSnackbar}
        anchorOrigin={{ vertical: 'top', horizontal: 'center' }}
      >
        <Alert 
          onClose={handleCloseSnackbar} 
          severity="success"
          sx={{ 
            width: '100%',
            bgcolor: 'white',
            color: 'success.main',
            border: 1,
            borderColor: 'success.main',
            '& .MuiAlert-icon': {
              color: 'success.main'
            }
          }}
        >
          {successMessage}
        </Alert>
      </Snackbar>
    </Container>
  );
};

export default Courses; 