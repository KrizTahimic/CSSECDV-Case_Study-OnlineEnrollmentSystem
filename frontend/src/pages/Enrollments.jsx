import React, { useState, useEffect } from 'react';
import { Container, Typography, Box, Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Paper, Button, CircularProgress } from '@mui/material';
import { useNavigate } from 'react-router-dom';

const Enrollments = () => {
  const [enrollments, setEnrollments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const navigate = useNavigate();

  useEffect(() => {
    const fetchEnrollments = async () => {
      try {
        const token = localStorage.getItem('token');
        const user = JSON.parse(localStorage.getItem('user'));
        
        if (!token) {
          navigate('/login');
          return;
        }

        const response = await fetch('http://localhost:3003/api/enrollments', {
          headers: {
            'Authorization': `Bearer ${token}`
          }
        });

        if (response.ok) {
          const data = await response.json();
          // Filter enrollments based on user role
          if (user.role === 'student') {
            setEnrollments(data.filter(enrollment => enrollment.studentId === user.id));
          } else {
            setEnrollments(data);
          }
        } else {
          setError('Failed to fetch enrollments');
        }
      } catch (err) {
        setError('Network error. Please try again.');
      } finally {
        setLoading(false);
      }
    };

    fetchEnrollments();
  }, [navigate]);

  const handleDrop = async (enrollmentId) => {
    try {
      const token = localStorage.getItem('token');
      
      const response = await fetch(`http://localhost:3003/api/enrollments/${enrollmentId}`, {
        method: 'DELETE',
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });

      if (response.ok) {
        // Remove the dropped enrollment from the list
        setEnrollments(enrollments.filter(enrollment => enrollment._id !== enrollmentId));
      } else {
        const data = await response.json();
        setError(data.message || 'Failed to drop enrollment');
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
          Enrollments
        </Typography>
        {error && (
          <Typography color="error" gutterBottom>
            {error}
          </Typography>
        )}
        <TableContainer component={Paper}>
          <Table>
            <TableHead>
              <TableRow>
                <TableCell>Course</TableCell>
                <TableCell>Student</TableCell>
                <TableCell>Enrollment Date</TableCell>
                <TableCell>Status</TableCell>
                <TableCell>Actions</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {enrollments.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={5} align="center">
                    No enrollments found
                  </TableCell>
                </TableRow>
              ) : (
                enrollments.map((enrollment) => (
                  <TableRow key={enrollment._id}>
                    <TableCell>{enrollment.courseId}</TableCell>
                    <TableCell>{enrollment.studentId}</TableCell>
                    <TableCell>{new Date(enrollment.enrollmentDate).toLocaleDateString()}</TableCell>
                    <TableCell>{enrollment.status}</TableCell>
                    <TableCell>
                      <Button
                        variant="outlined"
                        color="error"
                        size="small"
                        onClick={() => handleDrop(enrollment._id)}
                      >
                        Drop
                      </Button>
                    </TableCell>
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
        </TableContainer>
      </Box>
    </Container>
  );
};

export default Enrollments; 