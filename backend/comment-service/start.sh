#!/bin/bash

# Comment Service Startup Script
echo "🚀 Starting Comment Service..."

# Set environment variables
export SPRING_PROFILES_ACTIVE=dev
export SERVER_PORT=8084

# Start the service
./gradlew bootRun --args='--spring.profiles.active=dev'

echo "✅ Comment Service started on port 8084"
echo "📊 Health check: http://localhost:8084/actuator/health"
echo "📚 API Documentation: http://localhost:8084/swagger-ui.html"