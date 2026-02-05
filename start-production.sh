#!/bin/bash

echo "🌐 Web3 Community - Production Startup"
echo "=========================================="

# Set production environment
export COMPOSE_PROJECT_NAME=web3-community
export NODE_ENV=production

echo "📦 Building frontend..."
docker-compose build frontend

echo "🚀 Starting all services..."
echo "=========================================="

# Start infrastructure first
docker-compose up -d mysql redis eureka

echo "⏳ Waiting for Eureka..."
sleep 10

# Start application services
echo "🧑 Starting microservices..."
docker-compose up -d user-service auth-service post-service comment-service notification-service analytics-service

echo "🌐 Starting gateway and load balancer..."
docker-compose up -d api-gateway nginx

# Start frontend
docker-compose up -d frontend

echo "✅ All services started successfully!"
echo "=========================================="
echo ""
echo "🌐 Access URLs:"
echo "  Frontend: http://localhost:3000"
echo "  API Gateway: http://localhost:8080"
echo "  Eureka: http://localhost:8761"
echo "  Nginx: http://localhost:80"
echo ""
echo "📊 Health checks:"
echo "  Frontend: curl -f http://localhost:3000/health"
echo "  API Gateway: curl -f http://localhost:8080/actuator/health"
echo "  Eureka: curl -f http://localhost:8761/actuator/health"
echo ""
echo "📊 Service Health:"
echo "  User: curl -f http://localhost:8081/actuator/health"
echo "  Auth: curl -f http://localhost:8082/actuator/health"
echo "  Post: curl -f http://localhost:8083/actuator/health"
echo "  Comment: curl -f http://localhost:8084/actuator/health"
echo "  Notification: curl -f http://localhost:8085/actuator/health"
echo "  Analytics: curl -f http://localhost:8086/actuator/health"
echo ""
echo "📈 Production Logs:"
echo "  View logs: docker-compose logs -f [service-name]"
echo "  View all logs: docker-compose logs"
echo ""
echo "🛑 Stop all: ./stop-all.sh"
echo ""