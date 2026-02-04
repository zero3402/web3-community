#!/bin/bash

echo "🛑 Stopping Web3 Community services..."

# 모든 서비스 중지
docker-compose -f docker-compose.yml down

echo "✅ All services stopped"
echo "🗑️ Removing Docker network..."
docker network rm web3-network 2>/dev/null || echo "Network removed"

echo "💾 Optional: Remove volumes (uncomment if needed)"
# docker volume rm web3-mysql_data web3-redis_data 2>/dev/null || echo "Volumes removed"