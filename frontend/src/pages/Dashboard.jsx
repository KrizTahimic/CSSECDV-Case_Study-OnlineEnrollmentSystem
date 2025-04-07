import React, { useState, useEffect } from 'react';
import {
  Container,
  Typography,
  Box,
  Grid,
  Paper,
  Button,
  Alert
} from '@mui/material';
import { useNavigate } from 'react-router-dom';

const Dashboard = () => {
  const [error, setError] = useState('');
  const navigate = useNavigate();
  let user = { firstName: 'User' };
  
  try {
    const userData = localStorage.getItem('user');
    if (userData) {
      user = JSON.parse(userData);
    }
  } catch (error) {
    console.error('Error parsing user data:', error);
  }

  useEffect(() => {
    const token = localStorage.getItem('token');
    if (!token) {
      navigate('/login');
      return;
    }
  }, [navigate]);

  const handleLogout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    navigate('/login');
  };

  return (
    <Container>
      <Box sx={{ mt: 4, mb: 4 }}>
        <Typography variant="h4" component="h1" gutterBottom>
          Dashboard
        </Typography>
        <Typography variant="h5" gutterBottom>
          Welcome, {user.firstName || 'Professor'} {user.lastName || 'X'}!
        </Typography>
        <Button 
          variant="outlined" 
          color="primary" 
          onClick={handleLogout}
          sx={{ mb: 4 }}
        >
          LOGOUT
        </Button>
        {error && (
          <Alert severity="error" sx={{ mb: 2 }}>
            {error}
          </Alert>
        )}
        <Grid container spacing={3}>
          <Grid item xs={12} md={4}>
            <Paper
              sx={{
                p: 3,
                display: 'flex',
                flexDirection: 'column',
                height: '100%'
              }}
            >
              <Typography variant="h6" gutterBottom>
                Courses
              </Typography>
              <Typography variant="body1" sx={{ mb: 2 }}>
                View and manage available courses.
              </Typography>
              <Box sx={{ mt: 'auto' }}>
                <Button
                  variant="contained"
                  color="primary"
                  fullWidth
                  onClick={() => navigate('/courses')}
                >
                  GO TO COURSES
                </Button>
              </Box>
            </Paper>
          </Grid>
          <Grid item xs={12} md={4}>
            <Paper
              sx={{
                p: 3,
                display: 'flex',
                flexDirection: 'column',
                height: '100%'
              }}
            >
              <Typography variant="h6" gutterBottom>
                Enrollments
              </Typography>
              <Typography variant="body1" sx={{ mb: 2 }}>
                View and manage your course enrollments.
              </Typography>
              <Box sx={{ mt: 'auto' }}>
                <Button
                  variant="contained"
                  color="primary"
                  fullWidth
                  onClick={() => navigate('/enrollments')}
                >
                  GO TO ENROLLMENTS
                </Button>
              </Box>
            </Paper>
          </Grid>
          <Grid item xs={12} md={4}>
            <Paper
              sx={{
                p: 3,
                display: 'flex',
                flexDirection: 'column',
                height: '100%'
              }}
            >
              <Typography variant="h6" gutterBottom>
                Grades
              </Typography>
              <Typography variant="body1" sx={{ mb: 2 }}>
                View your grades and academic performance.
              </Typography>
              <Box sx={{ mt: 'auto' }}>
                <Button
                  variant="contained"
                  color="primary"
                  fullWidth
                  onClick={() => navigate('/grades')}
                >
                  GO TO GRADES
                </Button>
              </Box>
            </Paper>
          </Grid>
        </Grid>
      </Box>
    </Container>
  );
};

export default Dashboard;