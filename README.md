# AnimoSheesh - Online Enrollment System

A demonstration project showcasing distributed systems and fault tolerance concepts implemented using Java Spring Boot microservices architecture and a React frontend. This project is not intended for production use but serves as an educational example of microservices communication, fault tolerance patterns, and distributed system design.

## Educational Purpose

This system demonstrates several key concepts in distributed systems:
- Service discovery and registration
- Microservices architecture
- Fault tolerance and circuit breaking
- Distributed data management
- Service-to-service communication patterns
- Authentication and authorization in distributed systems

## System Architecture

The system consists of the following nodes:

1. **Service Discovery (Eureka Server)**
   - Port: 8761
   - Handles service registration and discovery
   - Displays which services are currently running

2. **Authentication Service**
   - Port: 3001
   - Handles user authentication and authorization
   - JWT-based authentication
   - User registration and login

3. **Course Service**
   - Port: 3002
   - Manages course information
   - Handles course enrollment capacity
   - Course CRUD operations

4. **Enrollment Service**
   - Port: 3003
   - Manages student course enrollments
   - Communicates with Course Service for enrollment validation
   - Handles enrollment and unenrollment operations

5. **Grade Service**
   - Port: 3004
   - Manages student grades
   - Faculty grade submission
   - Grade calculation and conversion

6. **Frontend (React)**
   - Port: 3000
   - Provides user interface for all system features
   - Integrates with all backend services
   - Built with Vite for optimal development experience

## Features

- User authentication and authorization (JWT)
- Role-based access control (Student, Faculty)
- Course management
- Student enrollment and course dropping
- Grade management and submission
- Fault tolerance with circuit breakers (Resilience4j)
- Service discovery and load balancing
- Responsive UI with Material-UI components

## Prerequisites

- Java 17 or higher
- Maven 3.6 or higher
- MongoDB
- Node.js 18 or higher (for frontend)
- npm 9 or higher

## Installation

1. Clone the repository:
   ```bash
   git clone https://github.com/CedricAlejo21/OnlineEnrollmentSystem-P4.git
   cd *insert-downloaded-repo-directory*
   ```

2. Install dependencies for each service:
   ```bash
   mvn clean compile
   
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

1. Start MongoDB:
   Ensure MongoDB is running on the default port (27017).

   If not installed:
      ```bash
      brew tap mongodb/brew
      brew install mongodb-community
      brew services start mongodb-community
      brew services start redis
      ```

2. Start the services in the following order:

   a. First, start the Service Discovery (Eureka Server):
   ```bash
   cd service-discovery
   mvn spring-boot:run
   ```
   Wait until the Eureka server starts successfully.

   b. Start the Authentication Service:
   ```bash
   cd auth-service
   mvn spring-boot:run
   ```

   c. Start the Course Service:
   ```bash
   cd course-service
   mvn spring-boot:run
   ```

   d. Start the Enrollment Service:
   ```bash
   cd enrollment-service
   mvn spring-boot:run
   ```

   e. Start the Grade Service:
   ```bash
   cd grade-service
   mvn spring-boot:run
   ```

   f. Start the Frontend:
   ```bash
   cd frontend
   npm start
   ```

## Verifying the Services

1. Check Eureka Dashboard:
   - Open http://localhost:8761 in your browser
   - You should see all services registered

2. Access the Frontend:
   - Open http://localhost:3000 in your browser

## User Roles and Functionalities

1. **Student**
   - View available courses
   - Enroll in courses
   - Drop enrolled courses
   - View grades

2. **Faculty**
   - View assigned courses
   - View enrolled students
   - Submit and update grades

3. **Admin**
   - Manage courses (create, update, delete)
   - Manage user accounts
   - View system statistics

## Database

Each service uses its own MongoDB database:
- auth_service: User authentication and profile data
- course_service: Course information
- enrollment_service: Student enrollment records
- grade_service: Student grade records

## Security

- JWT-based authentication
- Role-based access control
- Secure password storage (BCrypt)
- CORS configuration
- Input validation

### ⚠️ Important: JWT Token Configuration

**CRITICAL**: All services must use the same JWT secret for authentication to work properly.

**Configuration Files to Keep Synchronized:**
- `auth-service/src/main/resources/application.yml` 
- `auth-service/src/main/resources/application.properties`
- `course-service/src/main/resources/application.yml`
- `enrollment-service/src/main/resources/application.yml`
- `grade-service/src/main/resources/application.yml`
- All test configuration files (`application-test.yml`)

**When changing JWT secrets:**
1. Update the `jwt.secret` value in ALL configuration files listed above
2. Rebuild all services: `mvn clean compile` in each service directory
3. Restart all services in the correct order

**Common Issue:** If you get "JWT signature does not match" errors, it means services are using different JWT secrets. Check both `.yml` and `.properties` files in all services.

## Troubleshooting

1. If a service fails to start:
   - Check if the port is already in use
   - Verify MongoDB connection
   - Check the service logs for errors

2. If services can't communicate:
   - Verify Eureka Server is running
   - Check if services are registered in Eureka

3. If frontend can't connect to services:
   - Check if all services are running
   - Verify proxy settings in the frontend configuration