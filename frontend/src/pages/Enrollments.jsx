import React, { useState, useEffect } from 'react';
import { Container, Typography, Box, Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Paper, Button, CircularProgress } from '@mui/material';
import { useNavigate } from 'react-router-dom';

// Helper function to capitalize first letter
const capitalizeFirstLetter = (string) => {
  return string.charAt(0).toUpperCase() + string.slice(1).toLowerCase();
};

const Enrollments = () => {
  const [enrollments, setEnrollments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const navigate = useNavigate();

  useEffect(() => {
    const fetchEnrollments = async () => {
      try {
        const token = localStorage.getItem('token');
        if (!token) {
          navigate('/login');
          return;
        }

        const response = await fetch('http://localhost:3003/api/enrollment', {
          headers: {
            'Authorization': `Bearer ${token}`
          }
        });

        if (response.ok) {
          const data = await response.json();
          setEnrollments(data);
        } else {
          const errorData = await response.json();
          setError(errorData.message || 'Failed to fetch enrollments');
        }
      } catch (err) {
        console.error('Error fetching enrollments:', err);
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
      
      const response = await fetch(`http://localhost:3003/api/enrollment/${enrollmentId}/drop`, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });

      if (response.ok) {
        // Refresh enrollments after dropping
        const updatedEnrollment = await response.json();
        setEnrollments(enrollments.map(e => 
          e._id === updatedEnrollment._id ? updatedEnrollment : e
        ));
      } else {
        const data = await response.json();
        setError(data.message || 'Failed to drop enrollment');
      }
    } catch (err) {
      console.error('Error dropping enrollment:', err);
      setError('Network error. Please try again.');
    }
  };

  const handleReEnroll = async (enrollment) => {
    try {
      const token = localStorage.getItem('token');
      
      const response = await fetch('http://localhost:3003/api/enrollment', {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          course: enrollment.course._id
        })
      });

      if (response.ok) {
        const updatedEnrollment = await response.json();
        setEnrollments(enrollments.map(e => 
          e._id === updatedEnrollment._id ? updatedEnrollment : e
        ));
      } else {
        const data = await response.json();
        setError(data.message || 'Failed to re-enroll in course');
      }
    } catch (err) {
      console.error('Error re-enrolling:', err);
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
                    <TableCell>
                      {enrollment.course ? `${enrollment.course.code} - ${enrollment.course.title}` : 'N/A'}
                    </TableCell>
                    <TableCell>
                      {enrollment.student ? `${enrollment.student.firstName} ${enrollment.student.lastName}` : 'N/A'}
                    </TableCell>
                    <TableCell>{new Date(enrollment.enrollmentDate).toLocaleDateString()}</TableCell>
                    <TableCell>{capitalizeFirstLetter(enrollment.status)}</TableCell>
                    <TableCell>
                      {enrollment.status === 'enrolled' ? (
                        <Button
                          variant="outlined"
                          color="error"
                          size="small"
                          onClick={() => handleDrop(enrollment._id)}
                        >
                          Drop
                        </Button>
                      ) : enrollment.status === 'dropped' && (
                        <Button
                          variant="outlined"
                          color="primary"
                          size="small"
                          onClick={() => handleReEnroll(enrollment)}
                        >
                          Re-enroll
                        </Button>
                      )}
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