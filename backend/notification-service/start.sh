#!/bin/bash

# Notification Service Startup Script
echo "🚀 Starting Notification Service..."

# Set environment variables
export SPRING_PROFILES_ACTIVE=dev
export SERVER_PORT=8085

# Start the service
./gradlew bootRun --args='--spring.profiles.active=dev'

echo "✅ Notification Service started on port 8085"
echo "📊 Health check: http://localhost:8085/actuator/health"
echo "📚 API Documentation: http://localhost:8085/swagger-ui.html"
echo "🔌 WebSocket: ws://localhost:8085/ws/notifications"