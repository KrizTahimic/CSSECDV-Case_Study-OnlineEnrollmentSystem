#!/bin/bash

echo "=== Integration Test Diagnostics ==="
echo

# Check running containers
echo "Checking Docker containers:"
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" | grep -E "(eureka|auth|course|enrollment|grade)" || echo "No service containers running"

echo
echo "Checking container logs for errors:"

# Check for auth service issues
echo "--- Auth Service Logs ---"
docker ps -a | grep auth-service && docker logs $(docker ps -a | grep auth-service | awk '{print $1}') 2>&1 | tail -20 || echo "No auth service container found"

echo
echo "--- Course Service Logs ---"
docker ps -a | grep course-service && docker logs $(docker ps -a | grep course-service | awk '{print $1}') 2>&1 | tail -20 || echo "No course service container found"

echo
echo "=== Recommendations ==="
echo
echo "The integration tests are failing because Auth and Course services are not starting properly."
echo "This is likely due to:"
echo "1. Redis connection issues for Auth service"
echo "2. Service startup timing issues"
echo "3. Network connectivity between containers"
echo
echo "Workarounds:"
echo "1. Use mock profile for testing: mvn test -De2e.test.profile=mock"
echo "2. Run services manually with docker-compose and use manual profile"
echo "3. Use hybrid profile for partial integration testing"
echo
echo "To run with docker-compose:"
echo "  cd .. && docker-compose up -d"
echo "  cd security-e2e-tests && mvn test -De2e.test.profile=manual"