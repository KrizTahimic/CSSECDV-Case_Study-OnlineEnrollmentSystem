import React from 'react';
import { Routes, Route } from 'react-router-dom';
import { ThemeProvider, createTheme } from '@mui/material/styles';
import CssBaseline from '@mui/material/CssBaseline';
import Navbar from './components/Navbar.jsx';
import Home from './pages/Home.jsx';
import Login from './pages/Login.jsx';
import Register from './pages/Register.jsx';
import Dashboard from './pages/Dashboard.jsx';
import Courses from './pages/Courses.jsx';
import AddCourse from './pages/AddCourse.jsx';
import Enrollments from './pages/Enrollments.jsx';
import Grades from './pages/Grades.jsx';

const theme = createTheme({
  palette: {
    primary: {
      main: '#2e7d32', // Green 800
      light: '#4caf50', // Green 500
      dark: '#1b5e20', // Green 900
    },
    secondary: {
      main: '#81c784', // Green 300
      light: '#a5d6a7', // Green 200
      dark: '#66bb6a', // Green 400
    },
  },
});

function App() {
  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <Navbar />
      <Routes>
        <Route path="/" element={<Home />} />
        <Route path="/login" element={<Login />} />
        <Route path="/register" element={<Register />} />
        <Route path="/dashboard" element={<Dashboard />} />
        <Route path="/courses" element={<Courses />} />
        <Route path="/add-course" element={<AddCourse />} />
        <Route path="/enrollments" element={<Enrollments />} />
        <Route path="/grades" element={<Grades />} />
      </Routes>
    </ThemeProvider>
  );
}

export default App; 