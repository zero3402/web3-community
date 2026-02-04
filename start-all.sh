#!/bin/bash

echo "🚀 Web3 Community - Starting all services..."
echo "=================================================="

# 환경 변수 설정
export EMAIL_USERNAME=${EMAIL_USERNAME:-noreply@web3community.com}
export EMAIL_PASSWORD=${EMAIL_PASSWORD:-your_app_password}
export FIREBASE_SERVER_KEY=${FIREBASE_SERVER_KEY:-your_firebase_key}

# Docker 네트워크 생성
echo "📦 Creating Docker network..."
docker network create web3-network 2>/dev/null || echo "Network already exists"

# 볼륨륨과 데이터 볼륨 생성
echo "📁 Creating volumes..."
docker volume create web3-mysql_data 2>/dev/null || echo "MySQL volume already exists"
docker volume create web3-redis_data 2>/dev/null || echo "Redis volume already exists"

# 모든 서비스 시작
echo "🐳 Starting Infrastructure services..."
docker-compose -f docker-compose.yml up -d mysql redis

echo "⏳ Starting Eureka Service..."
sleep 10
docker-compose -f docker-compose.yml up -d eureka

echo "🌐 Starting API Gateway..."
sleep 15
docker-compose -f docker-compose.yml up -d api-gateway

echo "🧑 Starting Microservices..."
docker-compose -f docker-compose.yml up -d user-service auth-service post-service comment-service notification-service analytics-service

echo "📦 Starting Frontend..."
sleep 10
docker-compose -f docker-compose.yml up -d frontend

echo "🌐 Starting Nginx Load Balancer..."
docker-compose -f docker-compose.yml up -d nginx

echo "✅ All services started!"
echo "=================================================="
echo "🌐 Access URLs:"
echo "   Frontend: http://localhost:3000"
echo "   API Gateway: http://localhost:8080"
echo "   Eureka Dashboard: http://localhost:8761"
echo "   Nginx: http://localhost:80"
echo ""
echo "📊 Service Health Checks:"
echo "   User Service: http://localhost:8081/actuator/health"
echo "   Auth Service: http://localhost:8082/actuator/health"
echo "   Post Service: http://localhost:8083/actuator/health"
echo "   Comment Service: http://localhost:8084/actuator/health"
echo "   Notification Service: http://localhost:8085/actuator/health"
echo "   Analytics Service: http://localhost:8086/actuator/health"
echo "   API Gateway: http://localhost:8080/actuator/health"
echo ""
echo "💾 Logs:"
echo "   View logs: docker-compose logs [service-name]"
echo "   Stop all: docker-compose down"
echo "=================================================="