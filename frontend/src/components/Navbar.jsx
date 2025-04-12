import React from 'react';
import { Link as RouterLink, useNavigate } from 'react-router-dom';
import {
  AppBar,
  Toolbar,
  Typography,
  Button,
  Box,
  Container,
} from '@mui/material';
import { useAuth } from '../context/AuthContext';

const Navbar = () => {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
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
            {user ? (
              <>
                <Button color="inherit" component={RouterLink} to="/dashboard">
                  DASHBOARD
                </Button>
                <Button color="inherit" component={RouterLink} to="/courses">
                  {user.role?.toLowerCase() === 'faculty' ? 'MY COURSES' : 'COURSES'}
                </Button>
                {user.role?.toLowerCase() === 'student' && (
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