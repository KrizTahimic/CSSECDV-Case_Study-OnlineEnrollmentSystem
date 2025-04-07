const mongoose = require('mongoose');


// for testing purposes for MongoDB connection
async function testConnection() {
  try {
    console.log('Attempting to connect to MongoDB...');
    await mongoose.connect('mongodb://localhost:27017/test');
    console.log('✅ MongoDB connection successful - MongoDB is installed and running!');
    
    // Create a test collection
    const testSchema = new mongoose.Schema({
      name: String,
      created: { type: Date, default: Date.now }
    });
    
    const Test = mongoose.model('Test', testSchema);
    
    // Create a test document
    await Test.create({ name: 'Test entry' });
    console.log('✅ Successfully created test document');
    
    // Find the test document
    const doc = await Test.findOne({ name: 'Test entry' });
    console.log('✅ Found test document:', doc.name);
    
    // Clean up
    await mongoose.connection.dropCollection('tests');
    console.log('✅ Cleaned up test collection');
    
  } catch (error) {
    console.error('❌ MongoDB connection failed:', error.message);
    console.log('\n👉 MongoDB might not be installed or not running');
    console.log('👉 Install MongoDB: https://www.mongodb.com/try/download/community');
  } finally {
    if (mongoose.connection.readyState !== 0) {
      await mongoose.disconnect();
      console.log('👋 Disconnected from MongoDB');
    }
  }
}

testConnection();