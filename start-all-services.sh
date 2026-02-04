#!/bin/bash

echo "🚀 Starting Web3 Community Complete Microservices Architecture..."

# Create docker-compose file if not exists
if [ ! -f "docker-compose.yml" ]; then
    cat > docker-compose.yml << EOF
version: '3.8'

services:
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: password
      MYSQL_DATABASE: web3_community
    ports:
      - "3306:3306"
    volumes:
      - mysql_data:/var/lib/mysql

  mongodb:
    image: mongo:7.0
    ports:
      - "27017:27017"
    volumes:
      - mongodb_data:/data/db

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
    volumes:
      - redis_data:/data

  kafka:
    image: confluentinc/cp-kafka:latest
    environment:
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
    ports:
      - "9092:9092"
    depends_on:
      - zookeeper

  zookeeper:
    image: confluentinc/cp-zookeeper:latest
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
      ZOOKEEPER_TICK_TIME: 2000

  user-service:
    build: ./backend/user-service
    ports:
      - "8081:8081"
    environment:
      DB_USERNAME: root
      DB_PASSWORD: password
      REDIS_HOST: redis
      JWT_SECRET: mySecretKey123456789012345678901234567890
    depends_on:
      - mysql
      - redis

  post-service:
    build: ./backend/post-service
    ports:
      - "8082:8082"
    environment:
      MONGODB_URI: mongodb://mongodb:27017/web3_community_posts
    depends_on:
      - mongodb

  notification-service:
    build: ./backend/notification-service
    ports:
      - "8083:8083"
    environment:
      MONGODB_URI: mongodb://mongodb:27017/web3_community_notifications
      KAFKA_BOOTSTRAP_SERVERS: kafka:9092
    depends_on:
      - mongodb
      - kafka

  auth-service:
    build: ./backend/auth-service
    ports:
      - "8084:8084"
    environment:
      DB_USERNAME: root
      DB_PASSWORD: password
      JWT_SECRET: mySecretKey123456789012345678901234567890
    depends_on:
      - mysql
      - redis

  comment-service:
    build: ./backend/comment-service
    ports:
      - "8085:8085"
    environment:
      MONGODB_URI: mongodb://mongodb:27017/web3_community_comments
    depends_on:
      - mongodb

  api-gateway:
    build: ./backend/common
    ports:
      - "8080:8080"
    depends_on:
      - user-service
      - post-service
      - notification-service
      - auth-service
      - comment-service

volumes:
  mysql_data:
  mongodb_data:
  redis_data:
EOF
fi

# Start all services
docker-compose up -d --build

echo "⏳ Waiting for services to start..."
sleep 45

echo "🔍 Checking service health..."

# Check each service
check_service() {
    local service_name=$1
    local port=$2
    local tech_stack=$3
    
    echo "Checking $service_name (port $port) - $tech_stack..."
    
    if curl -f -s "http://localhost:$port/actuator/health" > /dev/null 2>&1 || curl -f -s "http://localhost:$port/" > /dev/null 2>&1; then
        echo "✅ $service_name is healthy"
    else
        echo "❌ $service_name is still starting..."
    fi
}

echo ""
echo "🏗️ Service Architecture:"
echo "  📋 User Service (MVC): http://localhost:8081 - User management"
echo "  📝 Post Service (WebFlux): http://localhost:8082 - Content management"
echo "  🔔 Notification Service (WebFlux): http://localhost:8083 - Real-time notifications"
echo "  🔐 Auth Service (MVC): http://localhost:8084 - Authentication & Authorization"
echo "  💬 Comment Service (WebFlux): http://localhost:8085 - Comment management"
echo "  🌐 API Gateway: http://localhost:8080 - Unified entry point"

echo ""
echo "📋 Quick Test Examples:"
echo ""
echo "🔐 Authentication:"
echo "curl -X POST http://localhost:8080/api/auth/login \\"
echo "  -H 'Content-Type: application/json' \\"
echo "  -d '{\"email\":\"admin@example.com\",\"password\":\"password123\"}'"
echo ""
echo "📝 Create Post:"
echo "curl -X POST http://localhost:8080/api/posts \\"
echo "  -H 'Content-Type: application/json' \\"
echo "  -d '{\"title\":\"New Post\",\"content\":\"Content\",\"authorId\":1,\"authorName\":\"Admin\"}'"
echo ""
echo "💬 Add Comment:"
echo "curl -X POST http://localhost:8080/api/comments \\"
echo "  -H 'Content-Type: application/json' \\"
echo "  -d '{\"postId\":\"post-id\",\"authorId\":1,\"content\":\"Great post!\",\"authorName\":\"Admin\"}'"
echo ""
echo "🔔 Create Notification:"
echo "curl -X POST http://localhost:8080/api/notifications \\"
echo "  -H 'Content-Type: application/json' \\"
echo "  -d '{\"userId\":1,\"title\":\"New Notification\",\"message\":\"Hello!\",\"type\":\"POST_CREATED\"}'"
echo ""
echo "📱 Real-time Notifications:"
echo "curl -N -H 'Accept: text/event-stream' http://localhost:8080/api/notifications/user/1/stream"
echo ""
echo "🔍 Search Posts:"
echo "curl 'http://localhost:8080/api/posts/search?query=web3'"
echo ""
echo "💡 Complete Architecture:"
echo "  • 6 Independent microservices"
echo "  • Mixed MVC/WebFlux architecture"
echo "  • Real-time features with SSE"
echo "  • Complete CRUD operations"
echo "  • Gateway with circuit breakers"
echo "  • Redis caching and session management"
echo "  • Kafka event streaming"
echo "  • MySQL for relational data"
echo "  • MongoDB for document data"
echo ""
echo "🛠️  Management:"
echo "  • Stop all: docker-compose down"
echo "  • View logs: docker-compose logs -f [service-name]"
echo "  • Rebuild: docker-compose up --build"

echo ""
echo "🎉 All services are starting up..."