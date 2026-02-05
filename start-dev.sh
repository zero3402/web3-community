#!/bin/bash

echo "🧑 Web3 Community - Development Startup"
echo "=========================================="

# Start infrastructure services
echo "🗃 Database and Cache..."
docker-compose up -d mysql redis

echo "⏳ Waiting for services to be ready..."
sleep 15

# Start service discovery
echo "🔍 Starting Eureka Service..."
docker-compose up -d eureka

echo "⚡ Starting API Gateway..."
docker-compose up -d api-gateway

# Start microservices
echo "👥 Starting User Service..."
docker-compose up -d user-service auth-service

echo "📝 Starting Content Services..."
docker-compose up -d post-service comment-service

echo "🔔 Starting Notification Service..."
docker-compose up -d notification-service

echo "📊 Starting Analytics Service..."
docker-compose up -d analytics-service

# Wait for all services to be ready
echo "⏳ Checking service health..."
sleep 30

# Start frontend
echo "🌐 Starting Frontend..."
docker-compose up -d frontend

echo "✅ Development environment ready!"
echo "=========================================="
echo ""
echo "🌐 Access URLs:"
echo "  Frontend: http://localhost:3000"
echo "  API Gateway: http://localhost:8080"
echo "  Eureka: http://localhost:8761"
echo ""
echo "📊 Service Health Checks:"
echo "  Frontend: curl -f http://localhost:3000/health || echo "Frontend not ready"
echo "  Gateway: curl -f http://localhost:8080/actuator/health || echo "Gateway not ready"
echo "  Eureka: curl -f http://localhost:8761/actuator/health || echo "Eureka not ready"
echo ""
echo "🛑 Debug logs:"
echo "  View all: docker-compose logs"
echo "  Stop all: ./stop-all.sh"
echo ""
echo "🔗 Quick access:"
echo "  Frontend logs: docker-compose logs -f frontend"
echo "  API Gateway: docker-compose logs -f api-gateway"
echo ""
echo "=================================================="