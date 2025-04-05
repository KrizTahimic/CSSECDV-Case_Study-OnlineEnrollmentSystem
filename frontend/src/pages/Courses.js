import React, { useState, useEffect } from 'react';
import { Container, Typography, Box, Grid, Card, CardContent, CardActions, Button, CircularProgress } from '@mui/material';
import { useNavigate } from 'react-router-dom';

const Courses = () => {
  const [courses, setCourses] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const navigate = useNavigate();

  useEffect(() => {
    const fetchCourses = async () => {
      try {
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

        if (response.ok) {
          const data = await response.json();
          setCourses(data);
        } else {
          setError('Failed to fetch courses');
        }
      } catch (err) {
        setError('Network error. Please try again.');
      } finally {
        setLoading(false);
      }
    };

    fetchCourses();
  }, [navigate]);

  const handleEnroll = async (courseId) => {
    try {
      const token = localStorage.getItem('token');
      const user = JSON.parse(localStorage.getItem('user'));

      if (user.role !== 'student') {
        setError('Only students can enroll in courses');
        return;
      }

      const response = await fetch('http://localhost:3003/api/enrollments', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify({
          studentId: user.id,
          courseId: courseId
        })
      });

      if (response.ok) {
        // Refresh courses or show success message
        setError('');
      } else {
        const data = await response.json();
        setError(data.message || 'Failed to enroll in course');
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
    <Container maxWidth="lg">
      <Box sx={{ mt: 4, mb: 4 }}>
        <Typography variant="h4" component="h1" gutterBottom>
          Available Courses
        </Typography>
        {error && (
          <Typography color="error" gutterBottom>
            {error}
          </Typography>
        )}
        <Grid container spacing={3}>
          {courses.map((course) => (
            <Grid item xs={12} sm={6} md={4} key={course._id}>
              <Card>
                <CardContent>
                  <Typography variant="h6" gutterBottom>
                    {course.title}
                  </Typography>
                  <Typography variant="body2" color="text.secondary">
                    {course.description}
                  </Typography>
                  <Typography variant="body2" sx={{ mt: 1 }}>
                    <strong>Instructor:</strong> {course.instructor}
                  </Typography>
                  <Typography variant="body2">
                    <strong>Credits:</strong> {course.credits}
                  </Typography>
                </CardContent>
                <CardActions>
                  <Button 
                    size="small" 
                    color="primary"
                    onClick={() => handleEnroll(course._id)}
                  >
                    Enroll
                  </Button>
                </CardActions>
              </Card>
            </Grid>
          ))}
        </Grid>
      </Box>
    </Container>
  );
};

export default Courses; 