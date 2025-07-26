#!/bin/bash

echo "=== Integration Test Prerequisites Check ==="
echo

# Check if Docker is running
echo -n "Checking Docker... "
if docker info >/dev/null 2>&1; then
    echo "✓ Docker is running"
else
    echo "✗ Docker is not running"
    echo "  Please start Docker Desktop or Docker daemon"
    exit 1
fi

# Check for required Docker images
echo
echo "Checking Docker images:"
IMAGES=(
    "onlineenrollmentsystem-p4-eureka:latest"
    "onlineenrollmentsystem-p4-auth-service:latest"
    "onlineenrollmentsystem-p4-course-service:latest"
    "onlineenrollmentsystem-p4-enrollment-service:latest"
    "onlineenrollmentsystem-p4-grade-service:latest"
)

MISSING_IMAGES=()
for IMAGE in "${IMAGES[@]}"; do
    if docker image inspect "$IMAGE" >/dev/null 2>&1; then
        echo "  ✓ $IMAGE"
    else
        echo "  ✗ $IMAGE (missing)"
        MISSING_IMAGES+=("$IMAGE")
    fi
done

if [ ${#MISSING_IMAGES[@]} -gt 0 ]; then
    echo
    echo "=== Missing Docker Images ==="
    echo "Some required images are missing. To build them:"
    echo
    echo "1. Build all services:"
    echo "   cd .. && mvn clean package"
    echo
    echo "2. Build Docker images:"
    echo "   docker-compose build"
    echo
    echo "Or use the build script if available:"
    echo "   ./build-docker-images.sh"
    exit 1
fi

# Check port availability
echo
echo "Checking port availability:"
PORTS=(8761 3001 3002 3003 3004)
PORTS_IN_USE=()

for PORT in "${PORTS[@]}"; do
    if lsof -i :$PORT >/dev/null 2>&1; then
        echo "  ✗ Port $PORT is already in use"
        PORTS_IN_USE+=($PORT)
    else
        echo "  ✓ Port $PORT is available"
    fi
done

if [ ${#PORTS_IN_USE[@]} -gt 0 ]; then
    echo
    echo "=== Ports In Use ==="
    echo "Some required ports are already in use."
    echo "Please stop any services using these ports."
    echo
    echo "To find what's using a port:"
    echo "  lsof -i :PORT_NUMBER"
    echo
    echo "Common causes:"
    echo "  - Docker containers still running"
    echo "  - Development services running locally"
    exit 1
fi

# Check Docker resources
echo
echo "Checking Docker resources:"
DOCKER_INFO=$(docker system df 2>/dev/null)
if [ $? -eq 0 ]; then
    echo "  ✓ Docker resources available"
else
    echo "  ⚠ Could not check Docker resources"
fi

# Final summary
echo
echo "=== Summary ==="
echo "✓ All prerequisites met!"
echo
echo "You can now run integration tests with:"
echo "  mvn test -De2e.test.profile=integration"
echo
echo "Or run a specific test:"
echo "  mvn test -Dtest=FullServiceE2ETest -De2e.test.profile=integration"
echo