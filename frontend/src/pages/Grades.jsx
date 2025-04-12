import React, { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import axios from 'axios';
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
  IconButton,
  FormControl,
  InputLabel,
  Select
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

      // Log user details to debug ID issues
      console.log('User from localStorage:', user);
      console.log('User ID:', user.id);
      console.log('User role:', user.role);

      let endpoint;
      // For students, use the specific endpoint with email
      if (user?.role?.toLowerCase() === 'student') {
        // Use email instead of ID
        const studentEmail = user.email;
        
        if (studentEmail) {
          endpoint = `${API_BASE_URLS.GRADE}/student/${studentEmail}`;
          console.log('Using student grades endpoint with email:', endpoint);
        } else {
          // If no email found, use general endpoint
          endpoint = API_BASE_URLS.GRADE;
          console.log('No student email found, using general grades endpoint');
        }
      } else {
        // For faculty or other roles, use general endpoint
        endpoint = API_BASE_URLS.GRADE;
        console.log('Using general grades endpoint for non-student role');
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
        
        // First, fetch all courses for populating course details
        const coursesResponse = await fetch(API_BASE_URLS.COURSE, {
          headers: {
            'Authorization': `Bearer ${token}`
          }
        });
        
        let allCourses = [];
        if (coursesResponse.ok) {
          allCourses = await coursesResponse.json();
          console.log('Fetched courses for populating grades:', allCourses);
        }
        
        // Populate course details and student information for each grade
        const populatedGrades = await Promise.all(data.map(async grade => {
          const course = allCourses.find(c => 
            c.id === grade.courseId || 
            c._id === grade.courseId
          );
          
          // Fetch student details if user is faculty
          let student = null;
          if (user.role && user.role.toLowerCase() === 'faculty' && grade.studentEmail) {
            try {
              const studentResponse = await fetch(`${API_BASE_URLS.AUTH}/users/email/${encodeURIComponent(grade.studentEmail)}`, {
                headers: {
                  'Authorization': `Bearer ${token}`
                }
              });
              
              if (studentResponse.ok) {
                student = await studentResponse.json();
                console.log('Fetched student details:', student);
              } else {
                console.error('Failed to fetch student details:', await studentResponse.text());
              }
            } catch (err) {
              console.error('Error fetching student details:', err);
            }
          }
          
          return {
            ...grade,
            course: course || null, 
            courseName: course ? `${course.code} - ${course.title}` : 'Unknown Course',
            student: student
          };
        }));
        
        // If faculty, filter grades for courses they teach
        if (user.role && user.role.toLowerCase() === 'faculty') {
          const facultyGrades = populatedGrades.filter(grade => {
            const course = grade.course;
            if (!course) return false;
            
            // Check if the faculty member is the instructor of this course
            return course.instructorId === user.id || 
                   (course.instructor && course.instructor.email === user.email);
          });
          
          console.log('Filtered faculty grades:', facultyGrades);
          setGrades(facultyGrades);
        } else {
          setGrades(populatedGrades);
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
      
      const token = localStorage.getItem('token');
      console.log('Fetching courses from:', API_BASE_URLS.COURSE);
      
      const response = await fetch(API_BASE_URLS.COURSE, {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });

      console.log('Course API response status:', response.status);
      
      if (response.ok) {
        const data = await response.json();
        console.log('Raw courses data:', data);
        
        if (!data || !Array.isArray(data) || data.length === 0) {
          console.log('No courses found or empty array returned');
          setCourses([]);
          return;
        }
        
        // Log example course to check structure
        if (data.length > 0) {
          console.log('Example course structure:', data[0]);
          console.log('Current user ID for comparison:', user.id);
          console.log('Current user email for comparison:', user.email);
        }
        
        // Try both direct comparison and case-insensitive email matching
        const facultyCourses = data.filter(course => {
          console.log(`Checking course ${course.code || 'unknown'}:`, {
            courseInstructorId: course.instructorId,
            userID: user.id,
            courseInstructorEmail: course.instructor?.email,
            userEmail: user.email
          });
          
          // Check by instructorId
          if (course.instructorId === user.id) {
            console.log(`Match by ID for course: ${course.code || 'unknown'}`);
            return true;
          }
          
          // Check by email (case insensitive)
          if (course.instructor && course.instructor.email && 
              course.instructor.email.toLowerCase() === user.email.toLowerCase()) {
            console.log(`Match by email for course: ${course.code || 'unknown'}`);
            return true;
          }
          
          // As a fallback, compare instructor ID with user ID from MongoDB format
          if (course.instructorId && user.id && course.instructorId.includes(user.id)) {
            console.log(`Partial ID match for course: ${course.code || 'unknown'}`);
            return true;
          }
          
          return false;
        });
        
        console.log('Filtered faculty courses count:', facultyCourses.length);
        
        // Only set the faculty's assigned courses
        setCourses(facultyCourses);
        
        // If no courses found, set appropriate message
        if (facultyCourses.length === 0) {
          setError('No course has been assigned to you. Please contact the administration.');
        }
      } else {
        const errorText = await response.text();
        console.error('Failed to fetch courses:', errorText);
        setError('Failed to fetch courses. Please try again.');
      }
    } catch (err) {
      console.error('Error fetching courses:', err);
      setError('Error loading courses: ' + err.message);
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
      
      // Reset students array
      setStudents([]);
      
      const response = await fetch(`${API_BASE_URLS.ENROLLMENT}/course/${courseId}`, {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });

      if (response.ok) {
        const data = await response.json();
        console.log('Enrollment data received:', data);
        
        if (!data || data.length === 0) {
          console.log('No enrollment data found for this course');
          setError('No students are enrolled in this course.');
          setLoading(false);
          return;
        }
        
        // First filter for enrolled status
        const enrollments = data.filter(enrollment => enrollment.status === 'enrolled');
        console.log('Filtered enrolled students:', enrollments.length);
        
        if (enrollments.length === 0) {
          setError('No active enrollments found for this course');
          setLoading(false);
          return;
        }

        // Process the enrollment data to extract student information
        const enrolledStudents = [];
        
        // For each enrollment, fetch the complete student data from auth service
        for (const enrollment of enrollments) {
          console.log('Processing enrollment:', enrollment);
          
          // Try to get student email from enrollment or use studentId if it's an email
          const studentEmail = enrollment.studentEmail || 
                             (enrollment.studentId && enrollment.studentId.includes('@') ? enrollment.studentId : null);
          
          if (studentEmail) {
            try {
              const studentResponse = await fetch(`${API_BASE_URLS.AUTH}/users/email/${encodeURIComponent(studentEmail)}`, {
                headers: {
                  'Authorization': `Bearer ${token}`
                }
              });
              
              if (studentResponse.ok) {
                const studentData = await studentResponse.json();
                console.log('Fetched student data:', studentData);
                
                // Add the student to our list with enrollment ID
                enrolledStudents.push({
                  id: studentData.email, // Use email as ID since that's what we need for grade submission
                  email: studentData.email,
                  firstName: studentData.firstName || 'Unknown',
                  lastName: studentData.lastName || 'Student',
                  enrollmentId: enrollment.id
                });
              } else {
                console.error('Failed to fetch student details:', await studentResponse.text());
                // Add a placeholder for the student
                enrolledStudents.push({
                  id: studentEmail,
                  email: studentEmail,
                  firstName: 'Unknown',
                  lastName: 'Student',
                  enrollmentId: enrollment.id,
                  error: true
                });
              }
            } catch (err) {
              console.error('Error fetching student data:', err);
              // Add a placeholder for the student
              enrolledStudents.push({
                id: studentEmail,
                email: studentEmail,
                firstName: 'Unknown',
                lastName: 'Student',
                enrollmentId: enrollment.id,
                error: true
              });
            }
          }
        }
        
        console.log('Final enrolled students list:', enrolledStudents);
        
        if (enrolledStudents.length === 0) {
          setError('No enrolled students found with details');
          setLoading(false);
          return;
        }
        
        setStudents(enrolledStudents);
      } else {
        const errorData = await response.json();
        console.error('Error fetching enrollments:', errorData);
        setError(errorData.message || 'Failed to fetch enrolled students');
      }
    } catch (err) {
      console.error('Error in fetchEnrolledStudents:', err);
      setError('Error fetching enrolled students: ' + err.message);
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
      
      console.log('Auth ME API response status:', response.status);
      
      if (response.ok) {
        const userData = await response.json();
        console.log('Current user details from auth service:', userData);
        
        // Update the user in localStorage with ID
        if (userData.id || userData._id) {
          const updatedUser = { ...user };
          updatedUser.id = userData.id || userData._id;
          
          // Ensure we have the correct role
          if (userData.role) {
            updatedUser.role = userData.role;
          }
          
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
    setError(''); // Clear any previous errors
    setOpenGradeDialog(true);
    setLoading(true);
    
    try {
      console.log('Opening grade dialog, fetching courses');
      
      // Get fresh token
      const token = localStorage.getItem('token');
      if (!token) {
        setError('Your session has expired. Please login again.');
        return;
      }
      
      // Get fresh user data
      const updatedUser = JSON.parse(localStorage.getItem('user'));
      console.log('Current user when opening dialog:', updatedUser);
      
      // Direct API call to courses
      console.log('Directly fetching courses for dialog');
      const courseResponse = await fetch(API_BASE_URLS.COURSE, {
        headers: { 'Authorization': `Bearer ${token}` }
      });
      
      if (courseResponse.ok) {
        const allCourses = await courseResponse.json();
        console.log('All courses fetched for dialog:', allCourses);
        
        if (allCourses.length > 0) {
          // Filter for instructor matching
          const facultyCourses = allCourses.filter(course => {
            // Check various ways the instructor might match
            const matchById = course.instructorId === updatedUser.id;
            const matchByEmail = course.instructor && 
                                 course.instructor.email && 
                                 course.instructor.email.toLowerCase() === updatedUser.email.toLowerCase();
            const mongoIdMatch = course.instructorId && 
                                 updatedUser.id && 
                                 course.instructorId.includes(updatedUser.id);
                                 
            return matchById || matchByEmail || mongoIdMatch;
          });
          
          console.log('Faculty courses for dialog filtered count:', facultyCourses.length);
          
          // Only set the faculty's assigned courses
          setCourses(facultyCourses);
          
          // Show error message if no courses are assigned to this faculty
          if (facultyCourses.length === 0) {
            setError('No course has been assigned to you. Please contact the administration.');
          }
        } else {
          console.log('No courses available from API');
          setCourses([]);
          setError('No courses are available in the system.');
        }
      } else {
        const errorText = await courseResponse.text();
        console.error('Error fetching courses for dialog:', errorText);
        setError('Failed to load courses. Server returned an error.');
      }
    } catch (err) {
      console.error('Exception during course fetch for dialog:', err);
      setError('Error loading courses: ' + err.message);
    } finally {
      setLoading(false);
    }
  };

  // Add a function to handle dialog close
  const handleCloseGradeDialog = () => {
    setOpenGradeDialog(false);
    setError(''); // Clear any errors
    setSelectedCourse(null); // Reset selected course
    setSelectedStudent(null); // Reset selected student
    setScore(''); // Reset score
    setComments(''); // Reset comments
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

      try {
        setLoading(true);
        
        // Grab the current user from localStorage and log it
        const storedUser = JSON.parse(localStorage.getItem('user'));
        console.log('Current user from localStorage on init:', storedUser);
        
        // If the user object doesn't have an ID, try to extract it from the token
        if (storedUser && (!storedUser.id || storedUser.id === 'undefined')) {
          try {
            // Get the token and try to decode it
            const token = localStorage.getItem('token');
            if (token) {
              // Import JWT decode for token extraction
              const { jwtDecode } = await import('jwt-decode');
              const decoded = jwtDecode(token);
              console.log('Decoded token:', decoded);
              
              // If the token has a userId claim, update the user object
              if (decoded.userId) {
                const updatedUser = {
                  ...storedUser,
                  id: decoded.userId
                };
                console.log('Updating user with ID from token:', updatedUser);
                localStorage.setItem('user', JSON.stringify(updatedUser));
                
                // Update our reference to user
                window.dispatchEvent(new Event('authStateChanged'));
              }
            }
          } catch (error) {
            console.error('Error decoding token:', error);
          }
        }
        
        // Try to fetch user details with ID from auth service
        const currentUser = await fetchCurrentUser();
        console.log('Fetched current user result:', currentUser);
        
        // After updating user info, get it again
        const updatedStoredUser = JSON.parse(localStorage.getItem('user'));
        console.log('Updated user from localStorage:', updatedStoredUser);
        
        // Directly test the course API
        console.log('Directly testing course API endpoint...');
        const courseResponse = await fetch(API_BASE_URLS.COURSE, {
          headers: { 'Authorization': `Bearer ${token}` }
        });
        
        if (courseResponse.ok) {
          const courseData = await courseResponse.json();
          console.log('Direct course API test - received courses count:', courseData.length);
          
          if (courseData.length > 0) {
            console.log('First course from direct API call:', courseData[0]);
          }
        } else {
          console.error('Direct course API test failed:', await courseResponse.text());
        }
        
        // Handle fetching based on user role
        if (updatedStoredUser && updatedStoredUser.role && 
            updatedStoredUser.role.toLowerCase() === 'faculty') {
          // For faculty users, load both courses and grades
          console.log('Faculty user confirmed, fetching courses and grades...');
          
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
        setError("Error loading data: " + error.message);
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
    // Validate inputs
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
        studentEmail: selectedStudent,
        courseId: selectedCourse,
        score: numericScore,
        facultyId: user.id,
        comments: comments
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
        
        // Close the dialog
        handleCloseGradeDialog();
        
        // Show success message
        setSuccessMessage(`Grade of ${numericScore}% submitted successfully for the student.`);
        setShowSuccess(true);
        
        // Refresh the grades list
        fetchGrades();
        
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
    setSelectedStudent(null); // Reset selected student when course changes
    
    if (courseId) {
      fetchEnrolledStudents(courseId);
    } else {
      setStudents([]); // Clear students if no course selected
    }
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

  // Replace the grade cards section with a table layout
  const renderGrades = () => {
    if (loading) {
      return <CircularProgress />;
    }

    if (error) {
      return (
        <Alert severity="error" sx={{ mt: 2 }}>
          {error}
        </Alert>
      );
    }

    if (grades.length === 0) {
      return (
        <Paper sx={{ p: 3, mt: 3, textAlign: 'center', borderTop: '4px solid #2e7d32' }}>
          <Typography variant="body1">No grades found</Typography>
        </Paper>
      );
    }

    return (
      <TableContainer component={Paper} sx={{ mt: 3, borderTop: '4px solid #2e7d32' }}>
        <Table>
          <TableHead>
            <TableRow sx={{ backgroundColor: '#f5f5f5' }}>
              <TableCell><strong>Course</strong></TableCell>
              {user.role && user.role.toLowerCase() === 'faculty' && (
                <TableCell><strong>Student</strong></TableCell>
              )}
              <TableCell><strong>Grade (GPA)</strong></TableCell>
              <TableCell><strong>Score</strong></TableCell>
              <TableCell><strong>Comments</strong></TableCell>
              <TableCell><strong>Submission Date</strong></TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {grades.map((grade) => {
              const course = grade.course || {};
              const scoreValue = parseFloat(grade.score) || 0;
              const gpaValue = getGpaFromScore(scoreValue);
              
              return (
                <TableRow key={grade.id || grade._id}>
                  <TableCell>
                    {course.code ? `${course.code} - ${course.title || ''}` : grade.courseName || 'Unknown Course'}
                  </TableCell>
                  {user.role && user.role.toLowerCase() === 'faculty' && (
                    <TableCell>
                      {grade.student ? 
                        `${grade.student.firstName} ${grade.student.lastName} (${grade.studentEmail})` : 
                        grade.studentEmail ? 
                          `Unknown Student (${grade.studentEmail})` : 
                          'Unknown Student'}
                    </TableCell>
                  )}
                  <TableCell>
                    <Chip
                      label={gpaValue.toFixed(1)}
                      sx={{
                        bgcolor: getGradeColor(gpaValue),
                        color: 'white',
                        fontWeight: 'bold'
                      }}
                    />
                  </TableCell>
                  <TableCell>{scoreValue.toFixed(1)}%</TableCell>
                  <TableCell>{grade.comments || '-'}</TableCell>
                  <TableCell>
                    {grade.submissionDate 
                      ? new Date(grade.submissionDate).toLocaleDateString() 
                      : '-'}
                  </TableCell>
                </TableRow>
              );
            })}
          </TableBody>
        </Table>
      </TableContainer>
    );
  };

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

        {renderGrades()}
      </Box>

      {/* Grade Submission Dialog */}
      <Dialog open={openGradeDialog} onClose={handleCloseGradeDialog} maxWidth="sm" fullWidth>
        <DialogTitle sx={{ borderBottom: '1px solid rgba(0, 0, 0, 0.12)', pb: 2 }}>
          <Typography variant="h5" component="div" fontWeight="bold">
            Submit Grade
          </Typography>
        </DialogTitle>
        <DialogContent>
          <Box component="form" sx={{ mt: 2, display: 'flex', flexDirection: 'column', gap: 2 }}>
            {error && (
              <Alert severity="error" sx={{ mb: 1 }} onClose={() => setError('')}>
                {error}
              </Alert>
            )}
            
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
              {loading ? (
                <MenuItem disabled>Loading courses...</MenuItem>
              ) : courses.length === 0 ? (
                <MenuItem disabled>No courses available</MenuItem>
              ) : (
                courses.map((course) => (
                  <MenuItem key={course._id || course.id} value={course._id || course.id}>
                    {course.code || 'No Code'} - {course.title || 'Untitled'}
                  </MenuItem>
                ))
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
              {!selectedCourse ? (
                <MenuItem disabled>Select a course first</MenuItem>
              ) : loading ? (
                <MenuItem disabled>Loading students...</MenuItem>
              ) : students.length === 0 ? (
                <MenuItem disabled>No students are currently enrolled in this course</MenuItem>
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
            onClick={handleCloseGradeDialog}
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