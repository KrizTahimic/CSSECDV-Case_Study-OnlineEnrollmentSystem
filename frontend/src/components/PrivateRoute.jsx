import React from 'react';
import { Navigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

const PrivateRoute = ({ children, roles }) => {
  const { isAuthenticated, user } = useAuth();

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  if (roles && !roles.some(role => role.toLowerCase() === user.role?.toLowerCase())) {
    return <Navigate to="/" replace />;
  }

  return children;
};

export default PrivateRoute; 