import React, { useState, useEffect } from 'react';
import {
  Container,
  Typography,
  Box,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  CircularProgress,
  Button,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  MenuItem,
  Alert,
  Tooltip,
  Grid,
  Card,
  CardContent,
  CardActions,
  Chip
} from '@mui/material';
import { useNavigate } from 'react-router-dom';
import API_BASE_URLS from '../config/api';

const getGradeFromScore = (score) => {
  if (score >= 95) return 4.0;
  if (score >= 89) return 3.5;
  if (score >= 83) return 3.0;
  if (score >= 78) return 2.5;
  if (score >= 72) return 2.0;
  if (score >= 66) return 1.5;
  if (score >= 60) return 1.0;
  return 0.0;
};

const getGpaFromScore = (score) => {
  if (score >= 95) return 4.0;
  if (score >= 89) return 3.5;
  if (score >= 83) return 3.0;
  if (score >= 78) return 2.5;
  if (score >= 72) return 2.0;
  if (score >= 66) return 1.5;
  if (score >= 60) return 1.0;
  return 0.0;
};

// Function to get letter grade
const getLetterGrade = (gpa) => {
  if (gpa >= 4.0) return 'A';
  if (gpa >= 3.5) return 'A-';
  if (gpa >= 3.0) return 'B+';
  if (gpa >= 2.5) return 'B';
  if (gpa >= 2.0) return 'B-';
  if (gpa >= 1.5) return 'C+';
  if (gpa >= 1.0) return 'C';
  return 'F';
};

// Function to get color for grade
const getGradeColor = (gpa) => {
  if (gpa >= 3.5) return '#2e7d32'; // Green for A/A-
  if (gpa >= 2.5) return '#1976d2'; // Blue for B+/B
  if (gpa >= 1.0) return '#ed6c02'; // Orange for B-/C+/C
  return '#d32f2f'; // Red for F
};

const GRADE_OPTIONS = ['A', 'A-', 'B+', 'B', 'B-', 'C+', 'C', 'F', 'I', 'W'];

const GradingScaleInfo = () => (
  <Box sx={{ display: 'flex', justifyContent: 'center', width: '100%', mb: 3 }}>
    <Card elevation={3} sx={{ maxWidth: 300, borderTop: '4px solid #2e7d32' }}>
      <CardContent>
        <Typography variant="h6" component="div" gutterBottom fontWeight="bold" textAlign="center">
          Grading Scale
        </Typography>
        <TableContainer>
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell align="center">Score Range</TableCell>
                <TableCell align="center">Grade</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              <TableRow>
                <TableCell align="center">95-100</TableCell>
                <TableCell align="center">4.0 (A)</TableCell>
              </TableRow>
              <TableRow>
                <TableCell align="center">89-94</TableCell>
                <TableCell align="center">3.5 (A-)</TableCell>
              </TableRow>
              <TableRow>
                <TableCell align="center">83-88</TableCell>
                <TableCell align="center">3.0 (B+)</TableCell>
              </TableRow>
              <TableRow>
                <TableCell align="center">78-82</TableCell>
                <TableCell align="center">2.5 (B)</TableCell>
              </TableRow>
              <TableRow>
                <TableCell align="center">72-77</TableCell>
                <TableCell align="center">2.0 (B-)</TableCell>
              </TableRow>
              <TableRow>
                <TableCell align="center">66-71</TableCell>
                <TableCell align="center">1.5 (C+)</TableCell>
              </TableRow>
              <TableRow>
                <TableCell align="center">60-65</TableCell>
                <TableCell align="center">1.0 (C)</TableCell>
              </TableRow>
              <TableRow>
                <TableCell align="center">Below 60</TableCell>
                <TableCell align="center">0.0 (F)</TableCell>
              </TableRow>
            </TableBody>
          </Table>
        </TableContainer>
      </CardContent>
    </Card>
  </Box>
);

const Grades = () => {
  const [grades, setGrades] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [openGradeDialog, setOpenGradeDialog] = useState(false);
  const [selectedCourse, setSelectedCourse] = useState(null);
  const [selectedStudent, setSelectedStudent] = useState(null);
  const [score, setScore] = useState('');
  const [comments, setComments] = useState('');
  const [courses, setCourses] = useState([]);
  const [students, setStudents] = useState([]);
  const navigate = useNavigate();
  const user = JSON.parse(localStorage.getItem('user'));

  const fetchGrades = async () => {
    try {
      const token = localStorage.getItem('token');
      if (!token) {
        navigate('/login');
        return;
      }

      const response = await fetch(API_BASE_URLS.GRADE, {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });

      if (response.ok) {
        const data = await response.json();
        console.log('Fetched grades:', data);
        setGrades(data);
      } else {
        const errorData = await response.json();
        setError(errorData.message || 'Failed to fetch grades');
      }
    } catch (err) {
      setError('Network error. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  const fetchCourses = async () => {
    if (user.role !== 'faculty') return;
    
    try {
      const token = localStorage.getItem('token');
      const response = await fetch(API_BASE_URLS.COURSE, {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });

      if (response.ok) {
        const data = await response.json();
        // Filter courses where the faculty is the instructor
        const facultyCourses = data.filter(course => course.instructor === user.id);
        setCourses(facultyCourses);
      }
    } catch (err) {
      console.error('Error fetching courses:', err);
    }
  };

  const fetchEnrolledStudents = async (courseId) => {
    try {
      const token = localStorage.getItem('token');
      console.log('Fetching enrolled students for course:', courseId);
      const response = await fetch(`${API_BASE_URLS.ENROLLMENT}/course/${courseId}`, {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });

      if (response.ok) {
        const data = await response.json();
        console.log('Enrollment data received:', data);
        
        // Use the student details already provided by the enrollment service
        const validStudents = data.map(enrollment => enrollment.student)
                                .filter(student => student && student._id);
        
        console.log('Processed student data:', validStudents);
        setStudents(validStudents);
      } else {
        console.error('Failed to fetch enrollments:', await response.text());
      }
    } catch (err) {
      console.error('Error fetching enrolled students:', err);
    }
  };

  useEffect(() => {
    fetchGrades();
    if (user.role === 'faculty') {
      fetchCourses();
    }
  }, [navigate]);

  const handleScoreChange = (e) => {
    const newScore = e.target.value;
    setScore(newScore);
  };

  const handleSubmitGrade = async () => {
    try {
      const numericScore = parseFloat(score);
      const calculatedGrade = getGradeFromScore(numericScore);
      
      const token = localStorage.getItem('token');
      const response = await fetch(API_BASE_URLS.GRADE, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          studentId: selectedStudent,
          courseId: selectedCourse,
          grade: calculatedGrade,
          score: numericScore,
          comments
        })
      });

      if (response.ok) {
        setSuccess('Grade submitted successfully');
        setOpenGradeDialog(false);
        fetchGrades();
        // Reset form
        setSelectedCourse(null);
        setSelectedStudent(null);
        setScore('');
        setComments('');
      } else {
        const errorData = await response.json();
        setError(errorData.message || 'Failed to submit grade');
      }
    } catch (err) {
      setError('Network error. Please try again.');
    }
  };

  const handleCourseChange = (courseId) => {
    setSelectedCourse(courseId);
    fetchEnrolledStudents(courseId);
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
        <Typography variant="h4" component="h1" gutterBottom fontWeight="bold">
          {user.role === 'faculty' ? 'Manage Grades' : 'My Grades'}
        </Typography>
        
        {error && (
          <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError('')}>
            {error}
          </Alert>
        )}
        
        {success && (
          <Alert severity="success" sx={{ mb: 2 }} onClose={() => setSuccess('')}>
            {success}
          </Alert>
        )}

        {user.role === 'faculty' && (
          <>
            <Box sx={{ mb: 3, width: '100%' }}>
              <Paper 
                elevation={3} 
                sx={{ 
                  p: 2, 
                  width: '100%', 
                  borderTop: '4px solid #2e7d32',
                  borderRadius: '4px' 
                }}
              >
                <Button
                  variant="contained"
                  sx={{ 
                    bgcolor: '#2e7d32',
                    '&:hover': { bgcolor: '#1b5e20' },
                    textTransform: 'uppercase',
                    fontWeight: 'bold'
                  }}
                  onClick={() => setOpenGradeDialog(true)}
                  fullWidth
                >
                  Submit New Grade
                </Button>
              </Paper>
            </Box>
            <GradingScaleInfo />
          </>
        )}

        <Grid container spacing={3}>
          {grades.length === 0 ? (
            <Grid item xs={12}>
              <Paper sx={{ p: 3, textAlign: 'center' }}>
                <Typography variant="body1">
                  No grades found
                </Typography>
              </Paper>
            </Grid>
          ) : (
            grades.map((grade) => (
              <Grid item xs={12} sm={6} md={4} key={grade._id}>
                <Card sx={{ 
                  height: '100%', 
                  display: 'flex', 
                  flexDirection: 'column',
                  overflow: 'visible',
                  borderTop: '4px solid',
                  borderColor: getGradeColor(grade.grade || 0)
                }}>
                  <CardContent sx={{ flexGrow: 1, pb: 1 }}>
                    <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 2 }}>
                      <Typography variant="h6" component="div">
                        {grade.course?.code}
                      </Typography>
                      <Chip 
                        label={grade.grade?.toFixed(1)} 
                        sx={{ 
                          bgcolor: getGradeColor(grade.grade || 0),
                          color: 'white',
                          fontWeight: 'bold'
                        }}
                      />
                    </Box>
                    <Typography variant="body1" gutterBottom>
                      <strong>{grade.course?.title}</strong>
                    </Typography>
                    
                    {user.role === 'faculty' && (
                      <Typography variant="body2" sx={{ mb: 1 }}>
                        <strong>Student:</strong> {grade.student?.firstName} {grade.student?.lastName}
                      </Typography>
                    )}
                    
                    <Typography variant="body2" sx={{ mb: 1 }}>
                      <strong>Score:</strong> {grade.score?.toFixed(1)}%
                    </Typography>
                    
                    <Typography variant="body2" sx={{ mb: 1 }}>
                      <strong>Grade:</strong> {grade.grade?.toFixed(1)} ({getLetterGrade(grade.grade || 0)})
                    </Typography>
                    
                    <Typography variant="body2" sx={{ mb: 1 }}>
                      <strong>Comments:</strong> {grade.comments || 'No comments'}
                    </Typography>
                    
                    <Typography variant="body2" sx={{ mb: 1, color: 'text.secondary' }}>
                      <strong>Submitted:</strong> {new Date(grade.submittedAt).toLocaleDateString()}
                    </Typography>
                  </CardContent>
                </Card>
              </Grid>
            ))
          )}
        </Grid>
      </Box>

      {/* Grade Submission Dialog */}
      <Dialog open={openGradeDialog} onClose={() => setOpenGradeDialog(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Submit Grade</DialogTitle>
        <DialogContent>
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, mt: 2 }}>
            <TextField
              select
              label="Course"
              value={selectedCourse || ''}
              onChange={(e) => handleCourseChange(e.target.value)}
              fullWidth
            >
              {courses.map((course) => (
                <MenuItem key={course._id} value={course._id}>
                  {course.code} - {course.title}
                </MenuItem>
              ))}
            </TextField>

            <TextField
              select
              label="Student"
              value={selectedStudent || ''}
              onChange={(e) => setSelectedStudent(e.target.value)}
              fullWidth
              disabled={!selectedCourse}
            >
              {students.map((student) => (
                <MenuItem key={student._id} value={student._id}>
                  {student.firstName} {student.lastName}
                </MenuItem>
              ))}
            </TextField>

            <TextField
              label="Score (0-100)"
              type="number"
              value={score}
              onChange={handleScoreChange}
              fullWidth
              inputProps={{ min: 0, max: 100, step: "0.1" }}
              helperText={score ? `Grade (GPA) will be: ${getGradeFromScore(parseFloat(score)).toFixed(1)} (${getLetterGrade(getGradeFromScore(parseFloat(score)))})` : ''}
            />

            <TextField
              label="Comments"
              multiline
              rows={4}
              value={comments}
              onChange={(e) => setComments(e.target.value)}
              fullWidth
            />
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpenGradeDialog(false)}>Cancel</Button>
          <Button
            onClick={handleSubmitGrade}
            variant="contained"
            sx={{ 
              bgcolor: '#2e7d32',
              '&:hover': { bgcolor: '#1b5e20' }
            }}
            disabled={!selectedCourse || !selectedStudent || !score}
          >
            Submit Grade
          </Button>
        </DialogActions>
      </Dialog>
    </Container>
  );
};

export default Grades; 