import React, { useState, useEffect } from 'react';
import { Container, Typography, Box, Grid, Paper, Button } from '@mui/material';
import { useNavigate } from 'react-router-dom';

const Dashboard = () => {
  const [user, setUser] = useState(null);
  const navigate = useNavigate();

  useEffect(() => {
    const storedUser = localStorage.getItem('user');
    if (storedUser) {
      const parsedUser = JSON.parse(storedUser);
      // Add detailed debugging
      console.log('Dashboard - User data:', {
        role: parsedUser.role,
        roleType: typeof parsedUser.role,
        isStudent: parsedUser.role === 'student',
        isFaculty: parsedUser.role === 'faculty',
        fullUser: parsedUser
      });
      setUser(parsedUser);
    } else {
      navigate('/login');
    }
  }, [navigate]);

  const handleLogout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    navigate('/login');
  };

  if (!user) {
    return null;
  }

  // Function to determine if enrollment should be shown
  const shouldShowEnrollment = () => {
    const shouldShow = user.role === 'student';
    console.log('Should show enrollment?', {
      currentRole: user.role,
      shouldShow: shouldShow
    });
    return shouldShow;
  };

  return (
    <Container maxWidth="lg">
      <Box sx={{ mt: 4, mb: 4 }}>
        <Typography variant="h4" component="h1" gutterBottom>
          Dashboard
        </Typography>
        <Typography variant="h6" gutterBottom>
          Welcome, {user.firstName} {user.lastName}!
        </Typography>
        <Button variant="outlined" color="primary" onClick={handleLogout} sx={{ mb: 3 }}>
          Logout
        </Button>

        <Grid container spacing={3}>
          <Grid item xs={12} md={4}>
            <Paper
              sx={{
                p: 3,
                display: 'flex',
                flexDirection: 'column',
                height: 240,
              }}
            >
              <Typography variant="h6" gutterBottom>
                Courses
              </Typography>
              <Typography variant="body1" paragraph>
                View and manage available courses.
              </Typography>
              <Button
                variant="contained"
                color="primary"
                onClick={() => navigate('/courses')}
                sx={{ mt: 'auto' }}
              >
                Go to Courses
              </Button>
            </Paper>
          </Grid>
          
          {/* Only show enrollment container for students */}
          {user.role === 'student' && (
            <Grid item xs={12} md={4}>
              <Paper
                sx={{
                  p: 3,
                  display: 'flex',
                  flexDirection: 'column',
                  height: 240,
                }}
              >
                <Typography variant="h6" gutterBottom>
                  Enrollments
                </Typography>
                <Typography variant="body1" paragraph>
                  View and manage your course enrollments.
                </Typography>
                <Button
                  variant="contained"
                  color="primary"
                  onClick={() => navigate('/enrollments')}
                  sx={{ mt: 'auto' }}
                >
                  Go to Enrollments
                </Button>
              </Paper>
            </Grid>
          )}

          <Grid item xs={12} md={4}>
            <Paper
              sx={{
                p: 3,
                display: 'flex',
                flexDirection: 'column',
                height: 240,
              }}
            >
              <Typography variant="h6" gutterBottom>
                Grades
              </Typography>
              <Typography variant="body1" paragraph>
                View your grades and academic performance.
              </Typography>
              <Button
                variant="contained"
                color="primary"
                onClick={() => navigate('/grades')}
                sx={{ mt: 'auto' }}
              >
                Go to Grades
              </Button>
            </Paper>
          </Grid>
        </Grid>
      </Box>
    </Container>
  );
};

export default Dashboard;