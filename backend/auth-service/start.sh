#!/bin/bash

# Auth Service Startup Script
echo "🚀 Starting Auth Service..."

# Set environment variables
export SPRING_PROFILES_ACTIVE=dev
export SERVER_PORT=8082

# Start the service
./gradlew bootRun --args='--spring.profiles.active=dev'

echo "✅ Auth Service started on port 8082"
echo "📊 Health check: http://localhost:8082/actuator/health"
echo "📚 API Documentation: http://localhost:8082/swagger-ui.html"