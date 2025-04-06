import React, { useState, useEffect } from 'react';
import { Container, Typography, Box, Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Paper, CircularProgress } from '@mui/material';
import { useNavigate } from 'react-router-dom';

const Grades = () => {
  const [grades, setGrades] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const navigate = useNavigate();

  useEffect(() => {
    const fetchGrades = async () => {
      try {
        const token = localStorage.getItem('token');
        const user = JSON.parse(localStorage.getItem('user'));
        
        if (!token) {
          navigate('/login');
          return;
        }

        const response = await fetch('http://localhost:3004/api/grades', {
          headers: {
            'Authorization': `Bearer ${token}`
          }
        });

        if (response.ok) {
          const data = await response.json();
          // Filter grades based on user role
          if (user.role === 'student') {
            setGrades(data.filter(grade => grade.studentId === user.id));
          } else if (user.role === 'faculty') {
            setGrades(data.filter(grade => grade.instructorId === user.id));
          } else {
            setGrades(data);
          }
        } else {
          setError('Failed to fetch grades');
        }
      } catch (err) {
        setError('Network error. Please try again.');
      } finally {
        setLoading(false);
      }
    };

    fetchGrades();
  }, [navigate]);

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
          Grades
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
                <TableCell>Grade</TableCell>
                <TableCell>Date</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {grades.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={4} align="center">
                    No grades found
                  </TableCell>
                </TableRow>
              ) : (
                grades.map((grade) => (
                  <TableRow key={grade._id}>
                    <TableCell>{grade.courseId}</TableCell>
                    <TableCell>{grade.studentId}</TableCell>
                    <TableCell>{grade.grade}</TableCell>
                    <TableCell>{new Date(grade.date).toLocaleDateString()}</TableCell>
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

export default Grades; 