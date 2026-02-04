#!/bin/bash

# Post Service Startup Script
echo "🚀 Starting Post Service..."

# Set environment variables
export SPRING_PROFILES_ACTIVE=dev
export SERVER_PORT=8083

# Start the service
./gradlew bootRun --args='--spring.profiles.active=dev'

echo "✅ Post Service started on port 8083"
echo "📊 Health check: http://localhost:8083/actuator/health"
echo "📚 API Documentation: http://localhost:8083/swagger-ui.html"