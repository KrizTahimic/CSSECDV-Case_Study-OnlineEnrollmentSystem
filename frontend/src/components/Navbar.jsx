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
      const userDataString = localStorage.getItem('user');
      let userData = null;

      try {
        if (userDataString) {
          userData = JSON.parse(userDataString);
        }
      } catch (error) {
        console.error('Error parsing user data:', error);
      }

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
    <AppBar position="static" sx={{ bgcolor: '#2e7d32' }}>
      <Container maxWidth="lg">
        <Toolbar sx={{ minHeight: '48px' }}>
          <Typography variant="h6" component={RouterLink} to="/" sx={{ 
            flexGrow: 1, 
            textDecoration: 'none', 
            color: 'inherit',
            display: 'flex',
            alignItems: 'center'
          }}>
            <img 
              src="/logo.png" 
              alt="AnimoSheesh Logo" 
              style={{ 
                height: '64px', 
                marginRight: '10px',
                verticalAlign: 'middle'
              }} 
            />
            AnimoSheesh!
          </Typography>
          <Box sx={{ 
            '& .MuiButton-root': { 
              textTransform: 'uppercase', 
              px: 2, 
              fontSize: '0.85rem',
              '&:hover': {
                bgcolor: 'rgba(255, 255, 255, 0.1)'
              }
            } 
          }}>
            {isAuthenticated && user ? (
              <>
                <Button color="inherit" component={RouterLink} to="/dashboard">
                  DASHBOARD
                </Button>
                <Button color="inherit" component={RouterLink} to="/courses">
                  {user.role === 'faculty' ? 'MY COURSES' : 'COURSES'}
                </Button>
                {user.role === 'student' && (
                  <Button color="inherit" component={RouterLink} to="/enrollments">
                    ENROLLMENTS
                  </Button>
                )}
                <Button color="inherit" component={RouterLink} to="/grades">
                  GRADES
                </Button>
                <Button color="inherit" onClick={handleLogout}>
                  LOGOUT {user.firstName ? `(${user.firstName.toUpperCase()})` : ''}
                </Button>
              </>
            ) : (
              <>
                <Button color="inherit" component={RouterLink} to="/login">
                  LOGIN
                </Button>
                <Button color="inherit" component={RouterLink} to="/register">
                  REGISTER
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