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
  let user = { firstName: 'User', role: 'student' };
  
  try {
    const userData = localStorage.getItem('user');
    if (userData) {
      user = JSON.parse(userData);
      console.log('User role:', user.role);
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

  const isFaculty = user.role && (user.role.toLowerCase() === 'faculty');

  return (
    <Container>
      <Box sx={{ mt: 4, mb: 4 }}>
        <Box sx={{ display: 'flex', alignItems: 'center', mb: 2 }}>
          <img 
            src="/logo.png" 
            alt="AnimoSheesh Logo" 
            style={{ 
              height: '40px', 
              marginRight: '12px',
              verticalAlign: 'middle'
            }} 
          />
          <Typography variant="h4" component="h1" gutterBottom fontWeight="bold" sx={{ ml: 1 }}>
            Dashboard
          </Typography>
        </Box>
        <Typography variant="h5" gutterBottom sx={{ color: '#2e7d32', mb: 1 }}>
          Welcome, {user.firstName || 'Professor'} {user.lastName || 'X'}!
        </Typography>
        
        {/* Display last login information (requirement 2.1.12) */}
        {user.lastLoginTime && (
          <Box sx={{ mb: 3, p: 2, bgcolor: '#f5f5f5', borderRadius: 1, border: '1px solid #e0e0e0' }}>
            <Typography variant="body2" sx={{ color: '#666', fontStyle: 'italic' }}>
              Last login: {new Date(user.lastLoginTime).toLocaleString()}
              {user.lastLoginIP && user.lastLoginIP !== 'Unknown' && (
                <> from {user.lastLoginIP}</>
              )}
            </Typography>
          </Box>
        )}
        
        {error && (
          <Alert severity="error" sx={{ mb: 2 }}>
            {error}
          </Alert>
        )}
        
        <Grid container spacing={3}>
          <Grid item xs={12} md={isFaculty ? 6 : 4}>
            <Paper
              elevation={3}
              sx={{
                p: 3,
                display: 'flex',
                flexDirection: 'column',
                height: '100%',
                borderTop: '4px solid #2e7d32',
                borderRadius: '4px'
              }}
            >
              <Typography variant="h6" gutterBottom fontWeight="bold">
                {isFaculty ? 'My Courses' : 'Courses'}
              </Typography>
              <Typography variant="body1" sx={{ mb: 3 }}>
                {isFaculty 
                  ? 'View and manage courses you are teaching. Track enrollments and student progress.'
                  : 'View and manage available courses. Browse the course catalog or add new courses.'}
              </Typography>
              <Box sx={{ mt: 'auto' }}>
                <Button
                  variant="contained"
                  sx={{ 
                    bgcolor: '#2e7d32',
                    '&:hover': { bgcolor: '#1b5e20' },
                    textTransform: 'uppercase',
                    fontWeight: 'bold'
                  }}
                  fullWidth
                  onClick={() => navigate('/courses')}
                >
                  GO TO {isFaculty ? 'MY COURSES' : 'COURSES'}
                </Button>
              </Box>
            </Paper>
          </Grid>
          
          {!isFaculty && (
            <Grid item xs={12} md={4}>
              <Paper
                elevation={3}
                sx={{
                  p: 3,
                  display: 'flex',
                  flexDirection: 'column',
                  height: '100%',
                  borderTop: '4px solid #2e7d32',
                  borderRadius: '4px'
                }}
              >
                <Typography variant="h6" gutterBottom fontWeight="bold">
                  Enrollments
                </Typography>
                <Typography variant="body1" sx={{ mb: 3 }}>
                  View and manage your course enrollments. Track your current courses and enrollment status.
                </Typography>
                <Box sx={{ mt: 'auto' }}>
                  <Button
                    variant="contained"
                    sx={{ 
                      bgcolor: '#2e7d32',
                      '&:hover': { bgcolor: '#1b5e20' },
                      textTransform: 'uppercase',
                      fontWeight: 'bold'
                    }}
                    fullWidth
                    onClick={() => navigate('/enrollments')}
                  >
                    GO TO ENROLLMENTS
                  </Button>
                </Box>
              </Paper>
            </Grid>
          )}
          
          <Grid item xs={12} md={isFaculty ? 6 : 4}>
            <Paper
              elevation={3}
              sx={{
                p: 3,
                display: 'flex',
                flexDirection: 'column',
                height: '100%',
                borderTop: '4px solid #2e7d32',
                borderRadius: '4px'
              }}
            >
              <Typography variant="h6" gutterBottom fontWeight="bold">
                Grades
              </Typography>
              <Typography variant="body1" sx={{ mb: 3 }}>
                {isFaculty
                  ? 'Manage and update student grades for your courses. Monitor class performance.'
                  : 'View your grades and academic performance. Monitor your progress in all enrolled courses.'}
              </Typography>
              <Box sx={{ mt: 'auto' }}>
                <Button
                  variant="contained"
                  sx={{ 
                    bgcolor: '#2e7d32',
                    '&:hover': { bgcolor: '#1b5e20' },
                    textTransform: 'uppercase',
                    fontWeight: 'bold'
                  }}
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