#!/bin/bash

# Script to run end-to-end tests with different modes

set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "$SCRIPT_DIR"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to check if a service is running
check_service() {
    local service_name=$1
    local port=$2
    local endpoint=${3:-"/actuator/health"}
    
    if curl -s -f "http://localhost:${port}${endpoint}" > /dev/null 2>&1; then
        echo -e "${GREEN}✓${NC} ${service_name} is running on port ${port}"
        return 0
    else
        echo -e "${RED}✗${NC} ${service_name} is not running on port ${port}"
        return 1
    fi
}

# Function to wait for service
wait_for_service() {
    local service_name=$1
    local port=$2
    local endpoint=${3:-"/actuator/health"}
    local max_attempts=30
    local attempt=1
    
    echo "Waiting for ${service_name} to start..."
    while [ $attempt -le $max_attempts ]; do
        if curl -s -f "http://localhost:${port}${endpoint}" > /dev/null 2>&1; then
            echo -e "${GREEN}✓${NC} ${service_name} is ready"
            return 0
        fi
        echo -n "."
        sleep 2
        ((attempt++))
    done
    echo -e "\n${RED}✗${NC} ${service_name} failed to start after $max_attempts attempts"
    return 1
}

# Parse command line arguments
MODE="manual"
SKIP_BUILD=false

while [[ $# -gt 0 ]]; do
    case $1 in
        --docker)
            MODE="docker"
            shift
            ;;
        --mock)
            MODE="mock"
            shift
            ;;
        --skip-build)
            SKIP_BUILD=true
            shift
            ;;
        --help)
            echo "Usage: $0 [options]"
            echo "Options:"
            echo "  --docker     Start services using Docker Compose"
            echo "  --mock       Run tests with mocked services (no real services needed)"
            echo "  --skip-build Skip building services (use with --docker)"
            echo "  --help       Show this help message"
            echo ""
            echo "Default mode: manual (expects services to be already running)"
            exit 0
            ;;
        *)
            echo "Unknown option: $1"
            exit 1
            ;;
    esac
done

echo "=== Online Enrollment System E2E Tests ==="
echo "Mode: ${MODE}"
echo ""

# Handle different modes
case $MODE in
    docker)
        echo "Starting services with Docker Compose..."
        
        # Build services if not skipping
        if [ "$SKIP_BUILD" = false ]; then
            echo "Building services..."
            mvn clean package -DskipTests
            
            # Create Dockerfiles if they don't exist
            for service in service-discovery auth-service course-service enrollment-service grade-service; do
                if [ ! -f "${service}/Dockerfile" ]; then
                    echo "Creating Dockerfile for ${service}..."
                    cat > "${service}/Dockerfile" << EOF
FROM openjdk:17-jdk-slim
COPY target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar"]
EOF
                fi
            done
        fi
        
        # Start services
        docker-compose -f docker-compose.test.yml up -d
        
        # Wait for services to be ready
        echo "Waiting for services to start..."
        wait_for_service "MongoDB" 27017 "/" || exit 1
        wait_for_service "Redis" 6379 "/" || exit 1
        wait_for_service "Eureka" 8761 "/actuator/health" || exit 1
        wait_for_service "Auth Service" 3001 "/actuator/health" || exit 1
        wait_for_service "Course Service" 3002 "/actuator/health" || exit 1
        wait_for_service "Enrollment Service" 3003 "/actuator/health" || exit 1
        wait_for_service "Grade Service" 3004 "/actuator/health" || exit 1
        
        echo -e "\n${GREEN}All services are ready!${NC}\n"
        ;;
        
    manual)
        echo "Checking if services are running..."
        
        all_running=true
        check_service "MongoDB" 27017 "/" || all_running=false
        check_service "Redis" 6379 "/" || all_running=false
        check_service "Eureka" 8761 "/actuator/health" || all_running=false
        check_service "Auth Service" 3001 "/actuator/health" || all_running=false
        check_service "Course Service" 3002 "/actuator/health" || all_running=false
        check_service "Enrollment Service" 3003 "/actuator/health" || all_running=false
        check_service "Grade Service" 3004 "/actuator/health" || all_running=false
        
        if [ "$all_running" = false ]; then
            echo -e "\n${YELLOW}Warning: Not all services are running.${NC}"
            echo "Please start missing services or use --docker mode."
            echo ""
            echo "To start services manually:"
            echo "  1. mongod"
            echo "  2. redis-server"
            echo "  3. cd service-discovery && mvn spring-boot:run"
            echo "  4. cd auth-service && mvn spring-boot:run"
            echo "  5. cd course-service && mvn spring-boot:run"
            echo "  6. cd enrollment-service && mvn spring-boot:run"
            echo "  7. cd grade-service && mvn spring-boot:run"
            exit 1
        fi
        
        echo -e "\n${GREEN}All services are running!${NC}\n"
        ;;
        
    mock)
        echo "Running tests with mocked services..."
        echo "No real services required."
        ;;
esac

# Run the tests
echo "Running E2E tests..."
cd security-e2e-tests

if [ "$MODE" = "mock" ]; then
    # Run only mock tests
    mvn test -Dtest=MockedE2ETest,TestInfrastructureTest
else
    # Run all E2E tests
    mvn test
fi

TEST_RESULT=$?

# Cleanup for docker mode
if [ "$MODE" = "docker" ]; then
    echo -e "\nStopping Docker services..."
    docker-compose -f ../docker-compose.test.yml down
fi

# Exit with test result
if [ $TEST_RESULT -eq 0 ]; then
    echo -e "\n${GREEN}✓ All tests passed!${NC}"
else
    echo -e "\n${RED}✗ Some tests failed!${NC}"
fi

exit $TEST_RESULT