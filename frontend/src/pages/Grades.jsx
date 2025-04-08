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
  Chip,
  Collapse,
  IconButton
} from '@mui/material';
import { useNavigate } from 'react-router-dom';
import API_BASE_URLS from '../config/api';
import CloseIcon from '@mui/icons-material/Close';

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
                <TableCell align="center">4.0</TableCell>
              </TableRow>
              <TableRow>
                <TableCell align="center">89-94</TableCell>
                <TableCell align="center">3.5</TableCell>
              </TableRow>
              <TableRow>
                <TableCell align="center">83-88</TableCell>
                <TableCell align="center">3.0</TableCell>
              </TableRow>
              <TableRow>
                <TableCell align="center">78-82</TableCell>
                <TableCell align="center">2.5</TableCell>
              </TableRow>
              <TableRow>
                <TableCell align="center">72-77</TableCell>
                <TableCell align="center">2.0</TableCell>
              </TableRow>
              <TableRow>
                <TableCell align="center">66-71</TableCell>
                <TableCell align="center">1.5</TableCell>
              </TableRow>
              <TableRow>
                <TableCell align="center">60-65</TableCell>
                <TableCell align="center">1.0</TableCell>
              </TableRow>
              <TableRow>
                <TableCell align="center">Below 60</TableCell>
                <TableCell align="center">0.0</TableCell>
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
  const [successMessage, setSuccessMessage] = useState('');
  const [showSuccess, setShowSuccess] = useState(false);
  const navigate = useNavigate();
  const user = JSON.parse(localStorage.getItem('user'));

  // Create a reference for the component's mounted state
  const isMountedRef = React.useRef(true);

  const fetchGrades = async () => {
    try {
      setLoading(true);
      const token = localStorage.getItem('token');
      if (!token) {
        navigate('/login');
        return;
      }

      let endpoint;
      // For students, try to use the specific endpoint
      if (user.role.toLowerCase() === 'student' && user.id) {
        endpoint = `${API_BASE_URLS.GRADE}/student/${user.id}`;
        console.log('Using student grades endpoint:', endpoint);
      } else {
        // For faculty or if student ID is missing, use general endpoint
        endpoint = API_BASE_URLS.GRADE;
        console.log('Using general grades endpoint:', endpoint);
      }

      console.log('Fetching grades from endpoint:', endpoint);
      const response = await fetch(endpoint, {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });

      if (response.ok) {
        const data = await response.json();
        console.log('Fetched grades:', data);
        
        // If faculty, filter grades for courses they teach
        if (user.role === 'faculty') {
          // Get all courses first if they haven't been loaded yet
          if (!courses || courses.length === 0) {
            await fetchCourses();
          }
          
          // Filter grades to only include those for courses taught by this faculty
          const facultyGrades = data.filter(grade => {
            // Get the course for this grade
            const associatedCourse = courses.find(
              course => course.id === grade.courseId || course._id === grade.courseId
            );
            
            if (!associatedCourse) {
              console.log('No matching course found for grade:', grade);
              return false;
            }
            
            console.log('Checking grade for course:', associatedCourse.code, 'instructor:', associatedCourse.instructor?.email);
            
            // Match if user is the instructor of this course
            const isInstructor = associatedCourse.instructor && 
                   associatedCourse.instructor.email && 
                   associatedCourse.instructor.email.toLowerCase() === user.email.toLowerCase();
                   
            if (isInstructor) {
              console.log('✓ This faculty teaches this course:', associatedCourse.code);
            }
            
            return isInstructor;
          });
          
          console.log('Filtered faculty grades by email:', facultyGrades);
          setGrades(facultyGrades);
        } else if (user.role === 'student' && endpoint === API_BASE_URLS.GRADE) {
          // If student using general endpoint, filter to only show their grades
          const studentGrades = data.filter(grade => 
            grade.studentId === user.id || 
            (grade.student && grade.student.email === user.email)
          );
          console.log('Filtered student grades:', studentGrades);
          setGrades(studentGrades);
        } else {
          // For other cases, use the data as is
          setGrades(data);
        }
      } else {
        console.error('Failed to fetch grades:', await response.json());
        setError('Failed to fetch grades. Please try again later.');
      }
    } catch (err) {
      console.error('Error fetching grades:', err);
      if (err.message.includes('Failed to fetch')) {
        setError('Unable to connect to the grade service. Please check if the service is running.');
      } else {
        setError('An unexpected error occurred while fetching grades: ' + err.message);
      }
    } finally {
      setLoading(false);
    }
  };

  const fetchCourses = async () => {
    if (!user || user.role !== 'faculty') return;
    
    try {
      setLoading(true);
      
      // Enhanced user logging with faculty ID focus
      console.log('Current faculty user ID check:', {
        id: user.id,
        email: user.email,
        specificMatch: user.id === '654f8e3f2b1c4d5e6f7a8b90' || user.email === 'marygrace.piattos@university.edu'
      });
      
      const token = localStorage.getItem('token');
      console.log('Fetching courses from:', API_BASE_URLS.COURSE);
      
      const response = await fetch(API_BASE_URLS.COURSE, {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });

      if (response.ok) {
        const data = await response.json();
        console.log('All courses fetched, count:', data.length);
        
        if (!data || !Array.isArray(data) || data.length === 0) {
          console.log('No courses found or empty array returned');
          setCourses([]);
          return;
        }
        
        // Directly search for VAL101 which is taught by Mary Grace
        const val101 = data.find(course => course.code === 'VAL101');
        if (val101) {
          console.log('Found VAL101 course:', val101);
          console.log('VAL101 instructor details:', {
            instructorId: val101.instructorId,
            instructorName: val101.instructor ? `${val101.instructor.firstName} ${val101.instructor.lastName}` : 'Unknown',
            instructorEmail: val101.instructor ? val101.instructor.email : 'Unknown'
          });
          
          // Check if Mary Grace is the instructor (by ID or email)
          const isMaryGraceInstructor = 
            (val101.instructorId === '654f8e3f2b1c4d5e6f7a8b90') || 
            (val101.instructor && val101.instructor.email === 'marygrace.piattos@university.edu');
          
          console.log('Is Mary Grace the instructor of VAL101:', isMaryGraceInstructor);
        }
        
        // Filter faculty courses - multiple approaches for debugging
        const facultyCourses = data.filter(course => {
          console.log(`Checking course ${course.code}:`, {
            courseInstructorId: course.instructorId,
            userID: user.id
          });
          
          // Direct ID check - use this first
          if (course.instructorId === user.id) {
            console.log(`✓ Match by ID for ${course.code} (${course.instructorId} = ${user.id})`);
            return true;
          }
          
          // Special case for Mary Grace Piattos 
          if (user.email === 'marygrace.piattos@university.edu' && 
              course.instructorId === '654f8e3f2b1c4d5e6f7a8b90') {
            console.log(`✓ Special match for ${course.code} - Mary Grace Piattos`);
            return true;
          }
          
          // Double-check instructor email
          if (course.instructor && course.instructor.email === user.email) {
            console.log(`✓ Match by email for ${course.code} (${course.instructor.email} = ${user.email})`);
            return true;
          }
          
          return false;
        });
        
        console.log('Filtered faculty courses:', facultyCourses);
        
        if (facultyCourses.length > 0) {
          setCourses(facultyCourses);
        } else {
          // For testing - include all courses if no matches found
          console.log('⚠️ No faculty matches found - setting all courses for testing');
          setCourses(data);
        }
      } else {
        console.error('Failed to fetch courses:', await response.text());
      }
    } catch (err) {
      console.error('Error fetching courses:', err);
    } finally {
      setLoading(false);
    }
  };

  const fetchEnrolledStudents = async (courseId) => {
    if (!courseId) return;
    
    try {
      setLoading(true);
      const token = localStorage.getItem('token');
      console.log(`Fetching enrolled students for course: ${courseId}`);
      
      // Temporary: Add test students immediately for testing
      const testStudents = [{
        id: 'test-student-id',
        _id: 'test-student-id',
        firstName: 'Gene Cedric',
        lastName: 'Alejo',
        email: 'genecedricalejo@gmail.com'
      }];
      
      // Set test students first so we have something
      setStudents(testStudents);
      
      const response = await fetch(`${API_BASE_URLS.ENROLLMENT}/course/${courseId}`, {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });

      if (response.ok) {
        const data = await response.json();
        console.log('Enrollment data received:', data);
        
        // Process the enrollment data to extract student information
        const enrolledStudents = data
          .filter(enrollment => enrollment.status === 'enrolled') // Only include active enrollments
          .map(enrollment => {
            // Extract student details from enrollment
            if (enrollment.student) {
              return {
                id: enrollment.studentId,
                _id: enrollment.studentId,
                firstName: enrollment.student.firstName,
                lastName: enrollment.student.lastName,
                email: enrollment.student.email
              };
            }
            return {
              id: enrollment.studentId,
              _id: enrollment.studentId,
              firstName: 'Unknown',
              lastName: 'Student',
              email: enrollment.studentEmail || 'unknown@example.com'
            };
          });
        
        console.log('Processed student data for grading:', enrolledStudents);
        
        if (enrolledStudents.length === 0) {
          console.log('No enrolled students found via enrollment service. Using test students.');
        } else {
          // Add real students to our list (leaving test students for backup)
          setStudents([...testStudents, ...enrolledStudents]);
        }
      } else {
        console.error('Failed to fetch enrollments:', await response.text());
        console.log('Using test students for grading.');
      }
    } catch (err) {
      console.error('Error fetching enrolled students:', err);
      console.log('Using test students for grading due to error.');
    } finally {
      setLoading(false);
    }
  };
  
  const fetchAllStudents = async () => {
    try {
      setLoading(true);
      const token = localStorage.getItem('token');
      
      console.log('Fetching all students from auth service');
      const response = await fetch(`${API_BASE_URLS.AUTH}/users?role=Student`, {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });
      
      if (response.ok) {
        const data = await response.json();
        console.log('All students data:', data);
        
        const formattedStudents = data.map(student => ({
          id: student.id,
          _id: student.id,
          firstName: student.firstName || 'Unknown',
          lastName: student.lastName || 'Student',
          email: student.email || 'unknown@example.com'
        }));
        
        console.log('Formatted student data:', formattedStudents);
        setStudents(formattedStudents);
      } else {
        console.error('Failed to fetch students from auth service:', await response.text());
        // Last resort - provide a test student
        const testStudent = {
          id: 'test-student-id',
          _id: 'test-student-id',
          firstName: 'Gene Cedric',
          lastName: 'Alejo',
          email: 'genecedricalejo@gmail.com'
        };
        setStudents([testStudent]);
      }
    } catch (err) {
      console.error('Error fetching all students:', err);
      setError('Error fetching students: ' + err.message);
    } finally {
      setLoading(false);
    }
  };

  // Function to get current user details including ID from auth service
  const fetchCurrentUser = async () => {
    try {
      const token = localStorage.getItem('token');
      if (!token) return null;
      
      console.log('Fetching current user details from auth service');
      const response = await fetch(API_BASE_URLS.AUTH_ME, {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });
      
      if (response.ok) {
        const userData = await response.json();
        console.log('Current user details from auth service:', userData);
        
        // Update the user in localStorage with ID
        if (userData.id || userData._id) {
          const updatedUser = { ...user };
          updatedUser.id = userData.id || userData._id;
          localStorage.setItem('user', JSON.stringify(updatedUser));
          
          console.log('Updated user in localStorage with ID:', updatedUser);
          return updatedUser;
        }
        
        return userData;
      } else {
        console.error('Failed to fetch current user details:', await response.text());
        return null;
      }
    } catch (error) {
      console.error('Error fetching current user:', error);
      return null;
    }
  };

  // Function to handle opening the grade dialog - ensure courses are loaded first
  const handleOpenGradeDialog = async () => {
    // If courses are already loaded, just open the dialog
    if (courses && courses.length > 0) {
      setOpenGradeDialog(true);
      return;
    }
    
    // Otherwise, load courses first
    setLoading(true);
    console.log('Loading courses before opening grade dialog...');
    
    try {
      await fetchCourses();
      // Now open the dialog
      setOpenGradeDialog(true);
    } catch (err) {
      console.error('Failed to load courses before opening dialog:', err);
      // Show alert if course loading fails
      setError('Failed to load courses. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    // Set mounted status to true on component mount
    isMountedRef.current = true;

    const fetchData = async () => {
      // Check if the user is logged in
      const token = localStorage.getItem('token');
      if (!token) {
        navigate('/login');
        return;
      }

      // Add more detailed logging of user data
      console.log('Current user from localStorage:', JSON.parse(localStorage.getItem('user')));
      console.log('User role:', user?.role);
      console.log('User ID:', user?.id);
      console.log('User email:', user?.email);

      try {
        setLoading(true);
        
        // Try to fetch user details with ID from auth service
        const currentUser = await fetchCurrentUser();
        
        // Handle fetching based on user role
        if (user && user.role && user.role.toLowerCase() === 'faculty') {
          // For faculty users, load both courses and grades
          console.log('Faculty user detected, fetching courses and grades...');
          
          // Fetch courses first
          await fetchCourses();
          
          // Only continue if component is still mounted
          if (isMountedRef.current) {
            // Then fetch grades
            await fetchGrades();
          }
        } else {
          // For students, just fetch grades
          console.log('Student user detected, fetching grades...');
          await fetchGrades();
        }
      } catch (error) {
        console.error("Error during initial data fetching:", error);
      } finally {
        setLoading(false);
      }
    };

    // Execute the fetch
    fetchData();

    // Cleanup function
    return () => {
      // Set mounted status to false when component unmounts
      isMountedRef.current = false;
    };
  }, []);

  const handleScoreChange = (e) => {
    const newScore = e.target.value;
    setScore(newScore);
  };

  const handleSubmitGrade = async () => {
    if (!selectedCourse || !selectedStudent) {
      setError('Please select both a course and a student.');
      return;
    }
    
    if (!score) {
      setError('Please enter a score.');
      return;
    }
    
    try {
      setLoading(true);
      const numericScore = parseFloat(score);
      
      if (isNaN(numericScore) || numericScore < 0 || numericScore > 100) {
        setError('Please enter a valid score between 0 and 100.');
        setLoading(false);
        return;
      }
      
      const token = localStorage.getItem('token');
      
      // Create the grade submission payload
      const gradeData = {
        studentId: selectedStudent,
        courseId: selectedCourse,
        score: numericScore,
        comments: comments,
        facultyId: user.id || user.email, // Use email as fallback
        facultyEmail: user.email, // Always include email for reference
        facultyName: `${user.firstName} ${user.lastName}` // Include name for reference
      };
      
      console.log('Submitting grade:', gradeData);
      
      const response = await fetch(API_BASE_URLS.GRADE, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(gradeData)
      });

      if (response.ok) {
        const data = await response.json();
        console.log('Grade submitted successfully:', data);
        setSuccess(`Grade of ${numericScore}% submitted successfully for the student.`);
        setOpenGradeDialog(false);
        
        // Refresh the grades list
        fetchGrades();
        
        // Reset the form
        setSelectedCourse(null);
        setSelectedStudent(null);
        setScore('');
        setComments('');
        setStudents([]);
        
        // Show success message
        setSuccessMessage('Grade submitted successfully!');
        setShowSuccess(true);
        
        // Hide success message after 5 seconds
        setTimeout(() => {
          if (isMountedRef.current) {
            setShowSuccess(false);
          }
        }, 5000);
      } else {
        const errorData = await response.json();
        console.error('Failed to submit grade:', errorData);
        setError(errorData.message || 'Failed to submit grade. Please try again.');
      }
    } catch (err) {
      console.error('Error submitting grade:', err);
      setError('Network error while submitting grade. Please check your connection and try again.');
    } finally {
      setLoading(false);
    }
  };

  const handleCourseChange = (courseId) => {
    setSelectedCourse(courseId);
    fetchEnrolledStudents(courseId);
  };

  const SuccessAlert = () => (
    <Collapse in={showSuccess}>
      <Alert 
        severity="success"
        action={
          <IconButton
            aria-label="close"
            color="inherit"
            size="small"
            onClick={() => setShowSuccess(false)}
          >
            <CloseIcon fontSize="inherit" />
          </IconButton>
        }
        sx={{ mb: 2 }}
      >
        {successMessage}
      </Alert>
    </Collapse>
  );

  if (loading) {
    return (
      <Container>
        <SuccessAlert />
        <Box sx={{ display: 'flex', justifyContent: 'center', mt: 4 }}>
          <CircularProgress />
        </Box>
      </Container>
    );
  }

  return (
    <Container maxWidth="lg">
      <SuccessAlert />
      <Box sx={{ mt: 4, mb: 4 }}>
        <Typography variant="h4" component="h1" gutterBottom fontWeight="bold">
          {user.role && user.role.toLowerCase() === 'faculty' ? 'Manage Grades' : 'My Grades'}
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

        {user.role && user.role.toLowerCase() === 'faculty' && (
          <Box sx={{ mb: 3, width: '100%' }}>
            <GradingScaleInfo />
            
            <Button
              variant="contained"
              sx={{ 
                bgcolor: '#2e7d32',
                '&:hover': { bgcolor: '#1b5e20' },
                width: '100%',
                py: 1.5,
                textTransform: 'uppercase',
                fontWeight: 'bold',
                mt: 3
              }}
              onClick={handleOpenGradeDialog}
            >
              Submit New Grade
            </Button>
          </Box>
        )}

        <Grid container spacing={3}>
          {grades.length === 0 ? (
            <Grid item xs={12}>
              <Paper sx={{ p: 3, textAlign: 'center', borderTop: '4px solid #2e7d32' }}>
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
                      <strong>Grade:</strong> {grade.grade?.toFixed(1)}
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
        <DialogTitle sx={{ borderBottom: '1px solid rgba(0, 0, 0, 0.12)', pb: 2 }}>
          <Typography variant="h5" component="div" fontWeight="bold">
            Submit Grade
          </Typography>
        </DialogTitle>
        <DialogContent>
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, mt: 2 }}>
            <TextField
              select
              label="Course"
              value={selectedCourse || ''}
              onChange={(e) => handleCourseChange(e.target.value)}
              fullWidth
              required
              error={!selectedCourse && Boolean(error)}
              helperText={!selectedCourse && Boolean(error) ? 'Please select a course' : ''}
            >
              {console.log('Course dropdown rendering, courses:', courses)}
              {courses.length === 0 ? (
                <MenuItem disabled>No courses available</MenuItem>
              ) : (
                courses.map((course) => {
                  console.log('Rendering course:', course.code, course);
                  return (
                    <MenuItem key={course.id || course._id} value={course.id || course._id}>
                      {course.code} - {course.title}
                    </MenuItem>
                  );
                })
              )}
            </TextField>
            
            <TextField
              select
              label="Student"
              value={selectedStudent || ''}
              onChange={(e) => setSelectedStudent(e.target.value)}
              fullWidth
              required
              disabled={!selectedCourse}
              error={selectedCourse && !selectedStudent && Boolean(error)}
              helperText={selectedCourse && !selectedStudent && Boolean(error) ? 'Please select a student' : ''}
            >
              {students.length === 0 ? (
                <MenuItem disabled>{selectedCourse ? 'No students enrolled' : 'Select a course first'}</MenuItem>
              ) : (
                students.map((student) => (
                  <MenuItem key={student._id || student.id} value={student._id || student.id}>
                    {student.firstName} {student.lastName} ({student.email})
                  </MenuItem>
                ))
              )}
            </TextField>
            
            <TextField
              label="Score (%)"
              type="number"
              value={score}
              onChange={handleScoreChange}
              fullWidth
              required
              inputProps={{ min: 0, max: 100, step: 0.1 }}
              error={!score && Boolean(error)}
              helperText={!score ? 'Enter a score between 0-100' : `Grade: ${getGpaFromScore(parseFloat(score) || 0).toFixed(1)}`}
            />
            
            <TextField
              label="Comments"
              value={comments}
              onChange={(e) => setComments(e.target.value)}
              fullWidth
              multiline
              rows={3}
              placeholder="Add optional comments about the student's performance"
            />
          </Box>
        </DialogContent>
        <DialogActions>
          <Button 
            onClick={() => setOpenGradeDialog(false)}
            disabled={loading}
            sx={{ 
              color: '#2e7d32',
              borderColor: '#2e7d32',
              '&:hover': { borderColor: '#1b5e20', backgroundColor: 'rgba(46, 125, 50, 0.04)' }
            }}
            variant="outlined"
          >
            CANCEL
          </Button>
          <Button 
            onClick={handleSubmitGrade} 
            variant="contained" 
            sx={{
              bgcolor: '#2e7d32',
              '&:hover': { bgcolor: '#1b5e20' },
              fontWeight: 'bold'
            }}
            disabled={loading || !selectedCourse || !selectedStudent || !score}
          >
            {loading ? 'SUBMITTING...' : 'SUBMIT GRADE'}
          </Button>
        </DialogActions>
      </Dialog>
    </Container>
  );
};

export default Grades; 