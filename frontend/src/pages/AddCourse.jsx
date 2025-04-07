import React, { useState, useEffect } from 'react';
import { 
  Container, 
  Typography, 
  Box, 
  TextField, 
  Button, 
  Paper, 
  Grid, 
  MenuItem, 
  FormControl, 
  InputLabel, 
  Select, 
  Chip,
  CircularProgress
} from '@mui/material';
import { useNavigate } from 'react-router-dom';

const AddCourse = () => {
  const [formData, setFormData] = useState({
    code: '',
    title: '',
    description: '',
    credits: 3,
    capacity: 30,
    schedule: {
      days: [],
      startTime: '09:00',
      endTime: '10:30',
      room: ''
    },
    prerequisites: []
  });
  const [prerequisite, setPrerequisite] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const navigate = useNavigate();

  useEffect(() => {
    const token = localStorage.getItem('token');
    const user = JSON.parse(localStorage.getItem('user'));
    
    if (!token) {
      navigate('/login');
      return;
    }
    
    if (user.role !== 'admin' && user.role !== 'faculty') {
      navigate('/dashboard');
    }
  }, [navigate]);

  const handleChange = (e) => {
    const { name, value } = e.target;
    if (name.includes('.')) {
      const [parent, child] = name.split('.');
      setFormData({
        ...formData,
        [parent]: {
          ...formData[parent],
          [child]: value
        }
      });
    } else {
      setFormData({
        ...formData,
        [name]: value
      });
    }
  };

  const handleDayChange = (day) => {
    const days = formData.schedule.days.includes(day)
      ? formData.schedule.days.filter(d => d !== day)
      : [...formData.schedule.days, day];
    
    setFormData({
      ...formData,
      schedule: {
        ...formData.schedule,
        days
      }
    });
  };

  const handleAddPrerequisite = () => {
    if (prerequisite && !formData.prerequisites.includes(prerequisite)) {
      setFormData({
        ...formData,
        prerequisites: [...formData.prerequisites, prerequisite]
      });
      setPrerequisite('');
    }
  };

  const handleRemovePrerequisite = (prereq) => {
    setFormData({
      ...formData,
      prerequisites: formData.prerequisites.filter(p => p !== prereq)
    });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError('');
    setSuccess('');

    // Validate required fields
    if (!formData.code.trim()) {
      setError('Course code is required');
      setLoading(false);
      return;
    }
    if (!formData.title.trim()) {
      setError('Course title is required');
      setLoading(false);
      return;
    }
    if (!formData.schedule.days.length) {
      setError('At least one day must be selected for the schedule');
      setLoading(false);
      return;
    }
    if (!formData.schedule.room.trim()) {
      setError('Room number is required');
      setLoading(false);
      return;
    }

    try {
      const token = localStorage.getItem('token');
      const user = JSON.parse(localStorage.getItem('user'));
      
      const response = await fetch('http://localhost:3002/api/courses', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify({
          ...formData,
          instructor: user.id
        })
      });

      if (response.ok) {
        setSuccess('Course created successfully');
        // Reset form
        setFormData({
          code: '',
          title: '',
          description: '',
          credits: 3,
          capacity: 30,
          schedule: {
            days: [],
            startTime: '09:00',
            endTime: '10:30',
            room: ''
          },
          prerequisites: []
        });
      } else {
        const data = await response.json();
        setError(data.message || 'Failed to create course');
      }
    } catch (err) {
      setError('Network error. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Container maxWidth="md">
      <Box sx={{ mt: 4, mb: 4 }}>
        <Typography variant="h4" component="h1" gutterBottom>
          Add New Course
        </Typography>
        
        {error && (
          <Typography color="error" gutterBottom>
            {error}
          </Typography>
        )}
        
        {success && (
          <Typography color="success.main" gutterBottom>
            {success}
          </Typography>
        )}
        
        <Paper elevation={3} sx={{ p: 4, mt: 2 }}>
          <Box component="form" onSubmit={handleSubmit}>
            <Grid container spacing={3}>
              <Grid item xs={12} sm={6}>
                <TextField
                  required
                  fullWidth
                  label="Course Code"
                  name="code"
                  value={formData.code}
                  onChange={handleChange}
                  placeholder="e.g., CS101"
                />
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField
                  required
                  fullWidth
                  label="Course Title"
                  name="title"
                  value={formData.title}
                  onChange={handleChange}
                  placeholder="e.g., Introduction to Programming"
                />
              </Grid>
              <Grid item xs={12}>
                <TextField
                  required
                  fullWidth
                  multiline
                  rows={3}
                  label="Description"
                  name="description"
                  value={formData.description}
                  onChange={handleChange}
                  placeholder="Course description"
                />
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField
                  required
                  fullWidth
                  type="number"
                  label="Credits"
                  name="credits"
                  value={formData.credits}
                  onChange={handleChange}
                  inputProps={{ min: 1, max: 6 }}
                />
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField
                  required
                  fullWidth
                  type="number"
                  label="Capacity"
                  name="capacity"
                  value={formData.capacity}
                  onChange={handleChange}
                  inputProps={{ min: 1 }}
                />
              </Grid>
              <Grid item xs={12}>
                <Typography variant="subtitle1" gutterBottom>
                  Schedule
                </Typography>
                <Box sx={{ mb: 2 }}>
                  {['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday'].map((day) => (
                    <Chip
                      key={day}
                      label={day}
                      onClick={() => handleDayChange(day)}
                      color={formData.schedule.days.includes(day) ? 'primary' : 'default'}
                      sx={{ m: 0.5 }}
                    />
                  ))}
                </Box>
                <Grid container spacing={2}>
                  <Grid item xs={12} sm={4}>
                    <TextField
                      required
                      fullWidth
                      label="Start Time"
                      name="schedule.startTime"
                      type="time"
                      value={formData.schedule.startTime}
                      onChange={handleChange}
                      InputLabelProps={{ shrink: true }}
                    />
                  </Grid>
                  <Grid item xs={12} sm={4}>
                    <TextField
                      required
                      fullWidth
                      label="End Time"
                      name="schedule.endTime"
                      type="time"
                      value={formData.schedule.endTime}
                      onChange={handleChange}
                      InputLabelProps={{ shrink: true }}
                    />
                  </Grid>
                  <Grid item xs={12} sm={4}>
                    <TextField
                      required
                      fullWidth
                      label="Room"
                      name="schedule.room"
                      value={formData.schedule.room}
                      onChange={handleChange}
                      placeholder="e.g., Room 101"
                    />
                  </Grid>
                </Grid>
              </Grid>
              <Grid item xs={12}>
                <Typography variant="subtitle1" gutterBottom>
                  Prerequisites
                </Typography>
                <Box sx={{ display: 'flex', mb: 2 }}>
                  <TextField
                    fullWidth
                    label="Prerequisite Course Code"
                    value={prerequisite}
                    onChange={(e) => setPrerequisite(e.target.value)}
                    placeholder="e.g., CS100"
                    sx={{ mr: 1 }}
                  />
                  <Button 
                    variant="outlined" 
                    onClick={handleAddPrerequisite}
                    disabled={!prerequisite}
                  >
                    Add
                  </Button>
                </Box>
                <Box sx={{ display: 'flex', flexWrap: 'wrap' }}>
                  {formData.prerequisites.map((prereq) => (
                    <Chip
                      key={prereq}
                      label={prereq}
                      onDelete={() => handleRemovePrerequisite(prereq)}
                      sx={{ m: 0.5 }}
                    />
                  ))}
                </Box>
              </Grid>
              <Grid item xs={12}>
                <Box sx={{ display: 'flex', justifyContent: 'space-between', mt: 2 }}>
                  <Button
                    variant="outlined"
                    onClick={() => navigate('/courses')}
                  >
                    Cancel
                  </Button>
                  <Button
                    type="submit"
                    variant="contained"
                    color="primary"
                    disabled={loading}
                  >
                    {loading ? <CircularProgress size={24} /> : 'Add Course'}
                  </Button>
                </Box>
              </Grid>
            </Grid>
          </Box>
        </Paper>
      </Box>
    </Container>
  );
};

export default AddCourse; 