import React, { useEffect } from 'react';
import { Container, Typography, Box, Button } from '@mui/material';
import { Link as RouterLink, useNavigate } from 'react-router-dom';

const Home = () => {
  const navigate = useNavigate();

  useEffect(() => {
    const token = localStorage.getItem('token');
    if (token) {
      navigate('/dashboard');
    }
  }, [navigate]);

  return (
    <Container maxWidth="md">
      <Box
        sx={{
          mt: 8,
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
        }}
      >
        <Typography variant="h2" component="h1" gutterBottom>
          Welcome to AnimoSheesh!
        </Typography>
        <Typography variant="h5" component="h2" gutterBottom color="text.secondary">
          A distributed system for managing course enrollments and grades
        </Typography>
        <Box sx={{ mt: 4 }}>
          <Button
            variant="contained"
            color="primary"
            size="large"
            component={RouterLink}
            to="/login"
            sx={{ mr: 2 }}
          >
            Login
          </Button>
          <Button
            variant="outlined"
            color="primary"
            size="large"
            component={RouterLink}
            to="/register"
          >
            Register
          </Button>
        </Box>
      </Box>
    </Container>
  );
};

export default Home; 