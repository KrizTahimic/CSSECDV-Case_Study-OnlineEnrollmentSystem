import React, { useState, useEffect } from 'react';
import { Container, Typography, Box, TextField, Button, Paper, MenuItem } from '@mui/material';
import { useNavigate } from 'react-router-dom';
import API_BASE_URLS from '../config/api';

const Register = () => {
  const [formData, setFormData] = useState({
    firstName: '',
    lastName: '',
    email: '',
    password: '',
    role: 'student', // Default to student
  });
  const [error, setError] = useState('');
  const navigate = useNavigate();

  useEffect(() => {
    const token = localStorage.getItem('token');
    if (token) {
      navigate('/dashboard');
    }
  }, [navigate]);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: value,
    }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');

    try {
      // Register the user
      const registerResponse = await fetch(`${API_BASE_URLS.AUTH}/register`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          firstName: formData.firstName,
          lastName: formData.lastName,
          email: formData.email,
          password: formData.password,
          role: formData.role
        }),
      });

      const registerData = await registerResponse.json();

      if (registerResponse.ok) {
        // Automatically log in the user
        const loginResponse = await fetch(`${API_BASE_URLS.AUTH}/login`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
          },
          body: JSON.stringify({
            email: formData.email,
            password: formData.password,
          }),
        });

        const loginData = await loginResponse.json();

        if (loginResponse.ok) {
          console.log('Login response after registration:', loginData);
          localStorage.setItem('token', loginData.token);
          localStorage.setItem('user', JSON.stringify({
            id: loginData.user?.id || loginData._id || loginData.id,
            firstName: loginData.user?.firstName || loginData.firstName,
            lastName: loginData.user?.lastName || loginData.lastName,
            email: loginData.user?.email || loginData.username || loginData.email,
            role: loginData.user?.role || loginData.role
          }));
          // Dispatch the authStateChanged event
          window.dispatchEvent(new Event('authStateChanged'));
          navigate('/dashboard');
        } else {
          // If login fails, show the error message from backend
          setError(loginData.message || 'Login failed after registration');
        }
      } else {
        setError(registerData.message || 'Registration failed');
      }
    } catch (err) {
      if (err.message && err.message.includes('fetch')) {
        setError('Network error. Please check if the service is running.');
      } else {
        setError(err.message || 'An unexpected error occurred. Please try again.');
      }
    }
  };

  return (
    <Container maxWidth="sm">
      <Box
        sx={{
          mt: 8,
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
        }}
      >
        <Paper elevation={3} sx={{ p: 4, width: '100%' }}>
          <Typography component="h1" variant="h5" align="center" gutterBottom>
            Register
          </Typography>
          {error && (
            <Typography color="error" align="center" gutterBottom>
              {error}
            </Typography>
          )}
          <Box component="form" onSubmit={handleSubmit} sx={{ mt: 1 }}>
            <TextField
              margin="normal"
              required
              fullWidth
              id="firstName"
              label="First Name"
              name="firstName"
              autoComplete="given-name"
              autoFocus
              value={formData.firstName}
              onChange={handleChange}
            />
            <TextField
              margin="normal"
              required
              fullWidth
              id="lastName"
              label="Last Name"
              name="lastName"
              autoComplete="family-name"
              value={formData.lastName}
              onChange={handleChange}
            />
            <TextField
              margin="normal"
              required
              fullWidth
              id="email"
              label="Email Address"
              name="email"
              autoComplete="email"
              value={formData.email}
              onChange={handleChange}
            />
            <TextField
              margin="normal"
              required
              fullWidth
              name="password"
              label="Password"
              type="password"
              id="password"
              autoComplete="new-password"
              value={formData.password}
              onChange={handleChange}
            />
            <TextField
              margin="normal"
              required
              fullWidth
              select
              name="role"
              label="Role"
              value={formData.role}
              onChange={handleChange}
            >
              <MenuItem value="student">Student</MenuItem>
              <MenuItem value="faculty">Faculty</MenuItem>
            </TextField>
            <Button
              type="submit"
              fullWidth
              variant="contained"
              sx={{ mt: 3, mb: 2 }}
            >
              Register
            </Button>
          </Box>
        </Paper>
      </Box>
    </Container>
  );
};

export default Register; 