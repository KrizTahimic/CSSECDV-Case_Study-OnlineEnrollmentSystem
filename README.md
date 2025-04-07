# Online Enrollment System

A distributed fault-tolerant online enrollment system demonstrating microservices architecture.

## Setup Instructions

1. Install MongoDB locally or use MongoDB Atlas
2. Clone this repository
3. Install dependencies for each node:

```bash
# Install dependencies for Auth Node
cd nodes/auth-node
npm install

# Install dependencies for Course Node
cd ../course-node
npm install

# Install dependencies for Enrollment Node
cd ../enrollment-node
npm install

# Install dependencies for Grade Node
cd ../grade-node
npm install

# Install dependencies for Frontend
cd ../../frontend
npm install
```

4. Create `.env` files in each node's directory using the provided `.env.example` files as templates
5. Start each service:

```bash
# Start Auth Service (port 3001)
cd nodes/auth-node
npm start

# Start Course Service (port 3002)
cd ../course-node
npm start

# Start Enrollment Service (port 3003)
cd ../enrollment-node
npm start

# Start Grade Service (port 3004)
cd ../grade-node
npm start

# Start Frontend (port 3000)
cd ../../frontend
npm start
```

## Features

- User authentication (login/logout)
- View available courses
- Student enrollment in courses
- View grades (for students)
- Upload grades (for faculty)

## Architecture

This system demonstrates distributed computing concepts:
- Each service runs on a separate node
- Services communicate via REST APIs
- Fault tolerance: If one service fails, others continue to function
- Each service has its own database

## Testing Fault Tolerance

To test the fault-tolerant nature of the system:
1. Stop any service (e.g., the course service)
2. Observe that other features continue to work
3. Course-related features will be unavailable
4. Restart the service to restore functionality

## Development

- Frontend: React with Material-UI
- Backend: Node.js with Express
- Database: MongoDB
- Authentication: JWT

## System Architecture

The system is divided into the following nodes:

1. **Auth Node** (Port 3001)
   - Handles user authentication and authorization
   - Manages user accounts and roles
   - JWT-based authentication

2. **Course Node** (Port 3002)
   - Manages course information
   - Handles course listings and details
   - Course capacity management

3. **Enrollment Node** (Port 3003)
   - Handles student course enrollment
   - Manages enrollment status
   - Validates enrollment requests

4. **Grade Node** (Port 3004)
   - Manages student grades
   - Handles grade submission by faculty
   - Grade history tracking

5. **Frontend Node** (Port 3000)
   - React-based user interface
   - Responsive design
   - Real-time updates

## Features

1. Node Isolation: Each node operates independently
2. JWT-based Authentication: Stateless authentication allows node restarts
3. Database Redundancy: Each node has its own database
4. Health Checks: Nodes monitor each other's status
5. Graceful Degradation: Nodes continue to function when others are down

## API Endpoints

Each node exposes its own REST API:

- Auth Node: `/api/auth/*`
- Course Node: `/api/courses/*`
- Enrollment Node: `/api/enrollment/*`
- Grade Node: `/api/grades/*`

## Getting Started

1. Install dependencies:
   ```bash
   npm install
   ```

2. ```bash
   npm run seed:all
   ```

3. Start the nodes from the project directory simultaneously:
   ```bash
   npm start
   ```

   Or start individual nodes:
   ```bash
   npm run start:auth
   npm run start:courses
   npm run start:enrollment
   npm run start:grades
   npm run start:frontend
   ```

4. Access the application at `http://localhost:3000`

## Fault Tolerance

The system is designed to be fault-tolerant:
- Each node operates independently
- If one node fails, others continue to function
- Database redundancy ensures data persistence
- Health checks monitor node status
- Automatic recovery when nodes are restored 
