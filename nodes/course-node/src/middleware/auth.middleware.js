require('dotenv').config();
const jwt = require('jsonwebtoken');

const verifyToken = (req, res, next) => {
  const token = req.headers['x-access-token'] || req.headers['authorization'];

  if (!token) {
    return res.status(403).send({
      message: 'No token provided!'
    });
  }

  try {
    const decoded = jwt.verify(token.replace('Bearer ', ''), process.env.JWT_SECRET);
    req.user = decoded;
    next();
  } catch (err) {
    return res.status(401).send({
      message: 'Unauthorized!'
    });
  }
};

const authorizeRole = (...roles) => {
  return (req, res, next) => {
    if (!req.user) {
      return res.status(403).send({
        message: 'No user found!'
      });
    }

    if (roles.includes(req.user.role) || req.user.role === 'admin') {
      next();
    } else {
      res.status(403).send({
        message: `Require ${roles.join(' or ')} Role!`
      });
    }
  };
};

const isAdmin = (req, res, next) => {
  if (req.user && req.user.role === 'admin') {
    next();
  } else {
    res.status(403).send({
      message: 'Require Admin Role!'
    });
  }
};

const isFaculty = (req, res, next) => {
  if (req.user && (req.user.role === 'faculty' || req.user.role === 'admin')) {
    next();
  } else {
    res.status(403).send({
      message: 'Require Faculty Role!'
    });
  }
};

const isStudent = (req, res, next) => {
  if (req.user && (req.user.role === 'student' || req.user.role === 'admin')) {
    next();
  } else {
    res.status(403).send({
      message: 'Require Student Role!'
    });
  }
};

module.exports = {
  verifyToken,
  authorizeRole,
  isAdmin,
  isFaculty,
  isStudent
}; 