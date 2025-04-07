# Online Enrollment System

A distributed online enrollment system built with Java Spring Boot microservices architecture.

## System Architecture

The system consists of the following microservices:

1. **Service Discovery (Eureka Server)**
   - Port: 8761
   - Handles service registration and discovery

2. **Authentication Service**
   - Port: 8081
   - Handles user authentication and authorization
   - JWT-based authentication
   - User registration and login

3. **Course Service**
   - Port: 8082
   - Manages course information
   - Handles course enrollment capacity
   - Course CRUD operations

4. **Enrollment Service**
   - Port: 8083
   - Manages student course enrollments
   - Communicates with Course Service for enrollment validation
   - Handles enrollment and unenrollment operations

5. **Grade Service**
   - Port: 8084
   - Manages student grades
   - Faculty grade submission
   - Grade calculation and conversion

## Features

- User authentication and authorization (JWT)
- Course management
- Student enrollment
- Grade management
- Fault tolerance with circuit breakers
- Service discovery and load balancing
- Distributed session management

## Prerequisites

- Java 17 or higher
- Maven 3.6 or higher
- Git
- Node.js 18 or higher (for frontend)

## Installation

1. Clone the repository:
   ```bash
   git clone https://github.com/yourusername/OnlineEnrollmentSystem-P4.git
   cd OnlineEnrollmentSystem-P4
   ```

2. Install dependencies for each service:
   ```bash
   # Install Service Discovery dependencies
   cd service-discovery
   mvn clean install

   # Install Authentication Service dependencies
   cd ../auth-service
   mvn clean install

   # Install Course Service dependencies
   cd ../course-service
   mvn clean install

   # Install Enrollment Service dependencies
   cd ../enrollment-service
   mvn clean install

   # Install Grade Service dependencies
   cd ../grade-service
   mvn clean install

   # Install Frontend dependencies
   cd ../frontend
   npm install
   ```

## Running the Services

Start the services in the following order:

1. First, start the Service Discovery (Eureka Server):
   ```bash
   cd service-discovery
   mvn spring-boot:run
   ```
   Wait until you see the Eureka server start up successfully (usually takes about 30 seconds).

2. In a new terminal, start the Authentication Service:
   ```bash
   cd auth-service
   mvn spring-boot:run
   ```

3. In another terminal, start the Course Service:
   ```bash
   cd course-service
   mvn spring-boot:run
   ```

4. In another terminal, start the Enrollment Service:
   ```bash
   cd enrollment-service
   mvn spring-boot:run
   ```

5. In another terminal, start the Grade Service:
   ```bash
   cd grade-service
   mvn spring-boot:run
   ```

6. Finally, in another terminal, start the Frontend:
   ```bash
   cd frontend
   npm start
   ```

## Verifying the Services

1. Check Eureka Dashboard:
   - Open http://localhost:8761 in your browser
   - You should see all services registered:
     - auth-service (UP)
     - course-service (UP)
     - enrollment-service (UP)
     - grade-service (UP)

2. Access the Frontend:
   - Open http://localhost:3000 in your browser
   - The frontend will automatically proxy requests to the appropriate services

## API Endpoints

### Authentication Service (8081)
- POST /api/auth/register - Register a new user
- POST /api/auth/login - User login

### Course Service (8082)
- GET /api/courses - Get all courses
- GET /api/courses/open - Get open courses
- GET /api/courses/{id} - Get course by ID
- POST /api/courses - Create a new course
- PUT /api/courses/{id} - Update a course
- DELETE /api/courses/{id} - Delete a course

### Enrollment Service (8083)
- GET /api/enrollments/student/{studentId} - Get student enrollments
- GET /api/enrollments/course/{courseId} - Get course enrollments
- POST /api/enrollments/student/{studentId}/course/{courseId} - Enroll a student
- DELETE /api/enrollments/student/{studentId}/course/{courseId} - Unenroll a student

### Grade Service (8084)
- GET /api/grades/student/{studentId} - Get student grades
- GET /api/grades/course/{courseId} - Get course grades
- GET /api/grades/faculty/{facultyId} - Get faculty grades
- POST /api/grades - Submit a grade
- PUT /api/grades/{id} - Update a grade
- DELETE /api/grades/{id} - Delete a grade

## Fault Tolerance

The system implements fault tolerance through:

1. Circuit Breakers (Resilience4j)
   - Prevents cascading failures
   - Implements fallback mechanisms
   - Configurable thresholds and timeouts

2. Service Discovery
   - Automatic service registration
   - Load balancing
   - Service health monitoring

3. Distributed Sessions
   - JWT-based authentication
   - Stateless service architecture
   - Session persistence across nodes

## Database

Each service uses its own H2 in-memory database for development. For production, you should:

1. Configure external databases
2. Implement database replication
3. Set up proper backup mechanisms

## Security

- JWT-based authentication
- Role-based access control
- Secure password storage (BCrypt)
- CORS configuration
- Input validation

## Troubleshooting

1. If a service fails to start:
   - Check if the port is already in use
   - Verify all dependencies are installed
   - Check the service logs for errors

2. If services can't communicate:
   - Verify Eureka Server is running
   - Check if services are registered in Eureka
   - Verify network connectivity

3. If frontend can't connect to services:
   - Check if all services are running
   - Verify proxy settings in vite.config.js
   - Check browser console for errors
