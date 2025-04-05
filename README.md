# Distributed Enrollment System

A fault-tolerant distributed enrollment system built with Express.js and microservices architecture.

## Architecture

The system is divided into the following microservices:

1. **Auth Service** (Port 3001)
   - Handles user authentication
   - JWT token generation and validation
   - User session management

2. **Course Service** (Port 3002)
   - Course management
   - Course listing and details
   - Course availability status

3. **Enrollment Service** (Port 3003)
   - Student enrollment management
   - Enrollment status tracking
   - Course registration

4. **Grade Service** (Port 3004)
   - Grade management
   - Grade upload for faculty
   - Grade viewing for students

5. **Frontend Service** (Port 3000)
   - User interface
   - Client-side routing
   - API integration

## Setup Instructions

1. Install dependencies:
   ```bash
   npm install
   ```

2. Set up environment variables:
   - Copy `.env.example` to `.env` in each service directory
   - Update the variables as needed

3. Start the services:
   ```bash
   npm start
   ```

## Fault Tolerance

The system implements fault tolerance through:

1. Service Isolation: Each service operates independently
2. JWT-based Authentication: Stateless authentication allows service restarts
3. Database Redundancy: Each service has its own database
4. Health Checks: Services monitor each other's status
5. Graceful Degradation: Services continue to function when others are down

## API Documentation

Each service exposes its own REST API:

- Auth Service: `/api/auth/*`
- Course Service: `/api/courses/*`
- Enrollment Service: `/api/enrollment/*`
- Grade Service: `/api/grades/*`

## Development

To run services individually:

```bash
npm run start:auth
npm run start:courses
npm run start:enrollment
npm run start:grades
npm run start:frontend
``` 