import React, { useState, useEffect } from 'react';
import {
  Container,
  Typography,
  Box,
  TextField,
  Button,
  MenuItem,
  Paper,
  CircularProgress,
  Alert
} from '@mui/material';
import { useNavigate } from 'react-router-dom';
import API_BASE_URLS from '../config/api';

const AddCourse = () => {
  const [formData, setFormData] = useState({
    code: '',
    title: '',
    description: '',
    credits: '',
    instructor: ''
  });
  const [instructors, setInstructors] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const navigate = useNavigate();
  const user = JSON.parse(localStorage.getItem('user'));

  useEffect(() => {
    const fetchInstructors = async () => {
      try {
        const token = localStorage.getItem('token');
        const response = await fetch(`${API_BASE_URLS.AUTH}/users?role=faculty`, {
          headers: {
            'Authorization': `Bearer ${token}`
          }
        });

        if (response.ok) {
          const data = await response.json();
          setInstructors(data);
        }
      } catch (err) {
        console.error('Error fetching instructors:', err);
      }
    };

    if (user.role === 'admin') {
      fetchInstructors();
    } else {
      navigate('/courses');
    }
  }, [navigate, user.role]);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: value
    }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError('');
    setSuccess('');

    try {
      const token = localStorage.getItem('token');
      const response = await fetch(API_BASE_URLS.COURSE, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(formData)
      });

      if (response.ok) {
        setSuccess('Course added successfully');
        setFormData({
          code: '',
          title: '',
          description: '',
          credits: '',
          instructor: ''
        });
      } else {
        const errorData = await response.json();
        setError(errorData.message || 'Failed to add course');
      }
    } catch (err) {
      setError('Network error. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  if (user.role !== 'admin') {
    return null;
  }

  return (
    <Container>
      <Box sx={{ mt: 4, mb: 4 }}>
        <Typography variant="h4" component="h1" gutterBottom>
          Add Course
        </Typography>
        {error && (
          <Alert severity="error" sx={{ mb: 2 }}>
            {error}
          </Alert>
        )}
        {success && (
          <Alert severity="success" sx={{ mb: 2 }}>
            {success}
          </Alert>
        )}
        <Paper sx={{ p: 3 }}>
          <Box component="form" onSubmit={handleSubmit}>
            <TextField
              fullWidth
              label="Course Code"
              name="code"
              value={formData.code}
              onChange={handleChange}
              margin="normal"
              required
            />
            <TextField
              fullWidth
              label="Title"
              name="title"
              value={formData.title}
              onChange={handleChange}
              margin="normal"
              required
            />
            <TextField
              fullWidth
              label="Description"
              name="description"
              value={formData.description}
              onChange={handleChange}
              margin="normal"
              multiline
              rows={4}
            />
            <TextField
              fullWidth
              label="Credits"
              name="credits"
              type="number"
              value={formData.credits}
              onChange={handleChange}
              margin="normal"
              required
            />
            <TextField
              select
              fullWidth
              label="Instructor"
              name="instructor"
              value={formData.instructor}
              onChange={handleChange}
              margin="normal"
              required
            >
              {instructors.map((instructor) => (
                <MenuItem key={instructor._id} value={instructor._id}>
                  {instructor.firstName} {instructor.lastName}
                </MenuItem>
              ))}
            </TextField>
            <Button
              type="submit"
              variant="contained"
              color="primary"
              sx={{ mt: 3 }}
              disabled={loading}
            >
              {loading ? <CircularProgress size={24} /> : 'Add Course'}
            </Button>
          </Box>
        </Paper>
      </Box>
    </Container>
  );
};

export default AddCourse; 