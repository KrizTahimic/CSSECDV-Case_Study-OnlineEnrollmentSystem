// Create a file called check-both-dbs.js
const { MongoClient } = require('mongodb');

async function checkBothDatabases() {
  const client = new MongoClient('mongodb://localhost:27017');
  
  try {
    await client.connect();
    console.log('Connected to MongoDB directly');
    
    // Check auth_service (with underscore)
    const db1 = client.db('auth_service');
    const users1 = await db1.collection('users').find({}).toArray();
    console.log(`\nDatabase 'auth_service' has ${users1.length} users:`);
    users1.forEach(u => console.log(`- ${u.email}`));
    
    // Check auth-service (with hyphen)
    const db2 = client.db('auth-service');
    const users2 = await db2.collection('users').find({}).toArray();
    console.log(`\nDatabase 'auth-service' has ${users2.length} users:`);
    users2.forEach(u => console.log(`- ${u.email}`));
    
  } catch (error) {
    console.error('Error:', error);
  } finally {
    await client.close();
  }
}

checkBothDatabases();