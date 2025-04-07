import React, { useState, useEffect } from 'react';
import { Link as RouterLink, useNavigate } from 'react-router-dom';
import {
  AppBar,
  Toolbar,
  Typography,
  Button,
  Box,
  Container,
} from '@mui/material';

const Navbar = () => {
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [user, setUser] = useState(null);
  const navigate = useNavigate();

  useEffect(() => {
    const checkAuth = () => {
      const token = localStorage.getItem('token');
      const userData = JSON.parse(localStorage.getItem('user'));
      setIsAuthenticated(!!token);
      setUser(userData);
    };

    // Check auth status on mount
    checkAuth();

    // Listen for auth state changes
    window.addEventListener('authStateChanged', checkAuth);

    // Cleanup listener on unmount
    return () => {
      window.removeEventListener('authStateChanged', checkAuth);
    };
  }, []);

  const handleLogout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    setIsAuthenticated(false);
    setUser(null);
    // Dispatch event to notify other components
    window.dispatchEvent(new Event('authStateChanged'));
    navigate('/');
  };

  return (
    <AppBar position="static">
      <Container maxWidth="lg">
        <Toolbar>
          <Typography variant="h6" component={RouterLink} to="/" sx={{ flexGrow: 1, textDecoration: 'none', color: 'inherit' }}>
            AnimoSheesh!
          </Typography>
          <Box>
            {isAuthenticated && user ? (
              <>
                <Button color="inherit" component={RouterLink} to="/dashboard">
                  Dashboard
                </Button>
                <Button color="inherit" component={RouterLink} to="/courses">
                  Courses
                </Button>
                {user.role === 'student' && (
                  <Button color="inherit" component={RouterLink} to="/enrollments">
                    My Enrollments
                  </Button>
                )}
                {(user.role === 'student' || user.role === 'faculty') && (
                  <Button color="inherit" component={RouterLink} to="/grades">
                    Grades
                  </Button>
                )}
                <Button color="inherit" onClick={handleLogout}>
                  Logout ({user.firstName})
                </Button>
              </>
            ) : (
              <>
                <Button color="inherit" component={RouterLink} to="/login">
                  Login
                </Button>
                <Button color="inherit" component={RouterLink} to="/register">
                  Register
                </Button>
              </>
            )}
          </Box>
        </Toolbar>
      </Container>
    </AppBar>
  );
};

export default Navbar; 