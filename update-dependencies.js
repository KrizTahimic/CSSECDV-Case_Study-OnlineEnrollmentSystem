const { execSync } = require('child_process');
const fs = require('fs');
const path = require('path');

const directories = [
  'services/auth-service',
  'services/course-service',
  'services/enrollment-service',
  'services/grade-service',
  'frontend'
];

// Function to update dependencies in a directory
function updateDependencies(dir) {
  console.log(`\nUpdating dependencies in ${dir}...`);
  try {
    // Update all dependencies to their latest versions
    execSync('npm install --save --legacy-peer-deps', { 
      cwd: dir,
      stdio: 'inherit'
    });
    
    // Update dev dependencies
    execSync('npm install --save-dev --legacy-peer-deps', { 
      cwd: dir,
      stdio: 'inherit'
    });
    
    console.log(`✅ Successfully updated dependencies in ${dir}`);
  } catch (error) {
    console.error(`❌ Error updating dependencies in ${dir}:`, error.message);
  }
}

// Update root dependencies
console.log('Updating root dependencies...');
try {
  execSync('npm install --save --legacy-peer-deps', { stdio: 'inherit' });
  execSync('npm install --save-dev --legacy-peer-deps', { stdio: 'inherit' });
  console.log('✅ Successfully updated root dependencies');
} catch (error) {
  console.error('❌ Error updating root dependencies:', error.message);
}

// Update dependencies in each service
directories.forEach(updateDependencies);

console.log('\nDependency update process completed!');
console.log('\nPlease run the following commands to verify the updates:');
console.log('1. npm audit');
console.log('2. npm outdated'); 