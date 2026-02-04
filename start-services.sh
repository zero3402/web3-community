#!/bin/bash

echo "🚀 Starting Clean Web3 Community Services..."

# Start all services with docker-compose
docker-compose up -d

echo "⏳ Waiting for services to start..."
sleep 30

echo "🔍 Checking service health..."

# Check each service
check_service() {
    local service_name=$1
    local port=$2
    local stack_type=$3
    
    echo "Checking $service_name (port $port) - $stack_type..."
    
    if curl -f -s "http://localhost:$port/" > /dev/null 2>&1; then
        echo "✅ $service_name is healthy"
    else
        echo "❌ $service_name is not responding (this may be expected if no root endpoint)"
    fi
}

# Check services with their stack types
check_service "API Gateway" 8080 "WebFlux"
check_service "User Service" 8081 "Spring MVC"
check_service "Post Service" 8082 "Spring WebFlux"
check_service "Notification Service" 8083 "Spring WebFlux"

echo ""
echo "🎉 Services are starting up!"
echo ""
echo "📋 Quick Test Examples:"
echo ""
echo "👤 User Service (MVC):"
echo "curl -X POST http://localhost:8080/api/auth/register \\"
echo "  -H 'Content-Type: application/json' \\"
echo "  -d '{\"username\":\"test\",\"email\":\"test@example.com\",\"password\":\"password123\"}'"
echo ""
echo "📝 Post Service (WebFlux):"
echo "curl -X POST http://localhost:8080/api/posts \\"
echo "  -H 'Content-Type: application/json' \\"
echo "  -d '{\"title\":\"Test\",\"content\":\"Content\",\"authorId\":1,\"authorName\":\"test\"}'"
echo ""
echo "🔔 Notification Service (WebFlux):"
echo "curl -X POST http://localhost:8080/api/notifications \\"
echo "  -H 'Content-Type: application/json' \\"
echo "  -d '{\"userId\":1,\"title\":\"New\",\"message\":\"Test\",\"type\":\"POST_CREATED\"}'"
echo ""
echo "📺 Real-time Notifications:"
echo "curl -N -H 'Accept: text/event-stream' http://localhost:8080/api/notifications/user/1/stream"
echo ""
echo "🔍 Search Posts:"
echo "curl 'http://localhost:8080/api/posts/search?query=test'"
echo ""
echo "💡 Services URLs:"
echo "  API Gateway: http://localhost:8080"
echo "  User Service: http://localhost:8081 (MVC)"
echo "  Post Service: http://localhost:8082 (WebFlux)"  
echo "  Notification Service: http://localhost:8083 (WebFlux)"
echo ""
echo "🛠️  To stop: docker-compose down"
echo "📋 To view logs: docker-compose logs -f [service-name]"