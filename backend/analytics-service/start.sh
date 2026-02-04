#!/bin/bash

# Analytics Service Startup Script
echo "🚀 Starting Analytics Service..."

# Set environment variables
export SPRING_PROFILES_ACTIVE=dev
export SERVER_PORT=8086

# Start the service
./gradlew bootRun --args='--spring.profiles.active=dev'

echo "✅ Analytics Service started on port 8086"
echo "📊 Health check: http://localhost:8086/actuator/health"
echo "📚 API Documentation: http://localhost:8086/swagger-ui.html"