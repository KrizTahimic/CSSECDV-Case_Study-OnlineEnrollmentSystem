# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a distributed microservices-based Online Enrollment System built with Java Spring Boot backend services and a React frontend. The system demonstrates fault tolerance, service discovery, and distributed systems concepts.

## Development Commands

### Backend Services (Maven)

Each microservice can be built and run independently:

```bash
# Build all services
mvn clean install

# Run individual services (from each service directory)
mvn spring-boot:run

# Run tests
mvn test

# Run a specific test class
mvn test -Dtest=ClassName

# Skip tests during build
mvn clean install -DskipTests
```

### Frontend (React/Vite)

```bash
# Install dependencies
npm install

# Start development server (port 3000)
npm start

# Build for production
npm run build

# Preview production build
npm run preview

# Run tests
npm run test
```
### Test 
```bash

mvn test

cd security-e2e-tests && mvn test -De2e.test.profile=integration
```

  
## Service Startup Order

Services must be started in this specific order:

1. **MongoDB** - Ensure MongoDB is running on port 27017
2. **Service Discovery (Eureka)** - Port 8761
3. **Auth Service** - Port 3001
4. **Course Service** - Port 3002
5. **Enrollment Service** - Port 3003
6. **Grade Service** - Port 3004
7. **Frontend** - Port 3000

Verify services are registered at: http://localhost:8761

## Architecture Overview

### Microservices Communication Pattern

- All services register with Eureka for service discovery
- Services communicate via REST APIs with JWT authentication
- Each service has its own MongoDB database
- Circuit breakers (Resilience4j) handle fault tolerance
- Services are configured to use localhost for local development

### Service Responsibilities

**Service Discovery (Eureka Server)**
- Central registry for all microservices
- Health monitoring and service routing

**Auth Service**
- User authentication/registration
- JWT token generation and validation
- User management (Student/Faculty/Admin roles)

**Course Service**
- Course CRUD operations
- Course capacity management
- Instructor assignment

**Enrollment Service**
- Student course enrollment/dropping
- Validates with Course Service for capacity
- Maintains enrollment records

**Grade Service**
- Grade submission by faculty
- Grade viewing by students
- Grade calculations

### Security Architecture

- JWT-based authentication across all services
- Each service validates JWT tokens independently
- CORS configured for frontend access
- BCrypt password hashing
- Role-based access control (RBAC)

### Key Configuration Points

- All services bind to `0.0.0.0` for network accessibility
- Eureka configured with localhost for service discovery
- JWT secrets must match across services
- MongoDB connection per service with separate databases
- Frontend API URLs use localhost for all service endpoints

### Database Schema

Each service manages its own MongoDB database:
- `auth_service`: Users collection
- `course_service`: Courses collection
- `enrollment_service`: Enrollments collection
- `grade_service`: Grades collection

## Common Development Tasks

### Adding a New Endpoint

1. Add controller method with appropriate mapping
2. Implement service layer logic
3. Add JWT authentication if needed
4. Update CORS if new origin required
5. Test with frontend integration

### Debugging Service Communication

1. Check Eureka dashboard for service registration
2. Verify JWT token is being passed in headers
3. Check service logs for circuit breaker status
4. Ensure MongoDB connections are active

### Frontend API Integration

- API endpoints defined in `frontend/src/config/api.js`
- Axios interceptors handle JWT token attachment
- AuthContext manages authentication state

## Code Quality Guidelines

#### Core Principles
- **KISS (Keep It Simple)**: Choose the simplest solution that meets requirements
- **YAGNI (You Ain't Gonna Need It)**: Don't add functionality until actually needed
- **No Backward Compatibility**: Prioritize clean code over maintaining old interfaces
- Delete obsolete code
- Fail fast and early. Avoid Fallbacks.
- **DRY (Don't Repeat Yourself)**: Extract repeated code into reusable functions
- Avoid over-engineering for hypothetical futures

### Problem-Solving Approach
- **Root cause analysis**: Avoid bandaid fixes and really fix the root of the problem
- **Systematic debugging**: Use proper debugging techniques rather than quick patches

### Documentation
- Comment the "why," not the "what"
- Use docstrings for functions and classes
- Keep comments current with code changes