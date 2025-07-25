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
PROFILE="hybrid"
SKIP_BUILD=false
TEST_FILTER=""

while [[ $# -gt 0 ]]; do
    case $1 in
        --profile)
            PROFILE="$2"
            shift 2
            ;;
        --mock)
            PROFILE="mock"
            shift
            ;;
        --integration)
            PROFILE="integration"
            shift
            ;;
        --hybrid)
            PROFILE="hybrid"
            shift
            ;;
        --manual)
            PROFILE="manual"
            shift
            ;;
        --skip-build)
            SKIP_BUILD=true
            shift
            ;;
        --test)
            TEST_FILTER="-Dtest=$2"
            shift 2
            ;;
        --help)
            echo "Usage: $0 [options]"
            echo "Options:"
            echo "  --profile <name>   Set test profile (mock|integration|hybrid|manual)"
            echo "  --mock             Use mock profile (WireMock for all services)"
            echo "  --integration      Use integration profile (real services in containers)"
            echo "  --hybrid           Use hybrid profile (infrastructure + mocks) [default]"
            echo "  --manual           Use manual profile (services already running)"
            echo "  --skip-build       Skip building services (for integration profile)"
            echo "  --test <pattern>   Run specific test(s) matching pattern"
            echo "  --help             Show this help message"
            echo ""
            echo "Profiles:"
            echo "  mock         - Fast testing with WireMock, no containers needed"
            echo "  integration  - Full E2E with all services in Docker containers"
            echo "  hybrid       - MongoDB/Redis containers + WireMock services"
            echo "  manual       - Expects all services to be manually started"
            echo ""
            echo "Examples:"
            echo "  $0 --mock                    # Quick security tests with mocks"
            echo "  $0 --integration             # Full integration tests"
            echo "  $0 --hybrid --test AuthenticationE2ETest  # Specific test with hybrid mode"
            exit 0
            ;;
        *)
            echo "Unknown option: $1"
            exit 1
            ;;
    esac
done

echo "=== Online Enrollment System E2E Tests ==="
echo "Profile: ${PROFILE}"
echo ""

# Handle different profiles
case $PROFILE in
    integration)
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
        echo "Running tests with mocked services (WireMock)..."
        echo "No real services or containers required."
        ;;
        
    hybrid)
        echo "Running tests with hybrid mode..."
        echo "MongoDB and Redis will run in containers, services will be mocked."
        ;;
esac

# Run the tests
echo "Running E2E tests..."
cd security-e2e-tests

# Run tests based on profile
if [ -n "$TEST_FILTER" ]; then
    # Run specific tests with profile
    mvn test -De2e.test.profile=$PROFILE $TEST_FILTER
else
    # Run tests based on profile
    case $PROFILE in
        mock)
            mvn test -De2e.test.profile=mock -Dtest=MockedE2ETest,TestInfrastructureTest
            ;;
        integration)
            mvn test -De2e.test.profile=integration -Dtest=AuthenticationE2ETest,AuthorizationE2ETest,SecurityAttackE2ETest,DataIntegrityE2ETest,FullServiceE2ETest
            ;;
        hybrid|manual)
            # Run all tests
            mvn test -De2e.test.profile=$PROFILE
            ;;
    esac
fi

TEST_RESULT=$?

# Cleanup for integration mode
if [ "$PROFILE" = "integration" ]; then
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