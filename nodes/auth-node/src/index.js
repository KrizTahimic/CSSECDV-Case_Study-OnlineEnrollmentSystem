require('dotenv').config();
const express = require('express');
const mongoose = require('mongoose');
const cors = require('cors');
const authRoutes = require('./routes/auth.routes');

const app = express();
const PORT = process.env.PORT || 3001;

// Middleware
app.use(cors());
app.use(express.json());

// Routes
app.use('/api/auth', authRoutes);

// Health check endpoint
app.get('/health', (req, res) => {
  res.status(200).json({ status: 'OK', service: 'auth-service' });
});

// Connect to MongoDB
mongoose.connect(process.env.MONGODB_URI || 'mongodb://localhost:27017/auth_node')
  .then(() => {
    console.log('=== MongoDB Connection Details ===');
    console.log('Connected to MongoDB');
    console.log('Database name:', mongoose.connection.db.databaseName);
    console.log('Connection host:', mongoose.connection.host);
    console.log('Connection port:', mongoose.connection.port);
    console.log('Connection string used:', process.env.MONGODB_URI || 'mongodb://localhost:27017/auth_node');
    console.log('=================================');
    
    // Check for users collection and content
    mongoose.connection.db.listCollections({ name: 'users' })
      .next((err, collinfo) => {
        if (collinfo) {
          console.log('Users collection exists in the connected database');
          mongoose.connection.db.collection('users').countDocuments()
            .then(count => {
              console.log(`Found ${count} users in the collection`);
              if (count > 0) {
                mongoose.connection.db.collection('users').find({}).toArray()
                  .then(users => {
                    console.log('User emails in database:');
                    users.forEach(u => console.log(`- ${u.email}`));
                  });
              }
            });
        } else {
          console.log('WARNING: Users collection does not exist in the connected database!');
        }
      });

    app.listen(PORT, () => {
      console.log(`Auth service running on port ${PORT}`);
    });
  })
  .catch((error) => {
    console.error('MongoDB connection error:', error);
  });

// Error handling middleware
app.use((err, req, res, next) => {
  console.error(err.stack);
  res.status(500).json({ error: 'Something went wrong!' });
}); 