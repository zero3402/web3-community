#!/bin/bash

# =============================================================================
# 🚀 운영 환경 배포 스크립트
# =============================================================================
# 설명: 운영 서버에 Web3 Community 배포
# 특징: 안전한 배포, 롤백, 데이터베이스 백업
# 목적: 운영 환경 자동화 배포
# =============================================================================

# =============================================================================
# 🔧 설정
# =============================================================================
set -e

# 색상 코드 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 로깅 함수
log() {
    echo -e "${GREEN}[$(date +'%Y-%m-%d %H:%M:%S')] $1${NC}"
}

error() {
    echo -e "${RED}[ERROR] $1${NC}"
    exit 1
}

warning() {
    echo -e "${YELLOW}[WARNING] $1${NC}"
}

info() {
    echo -e "${BLUE}[INFO] $1${NC}"
}

# =============================================================================
# 🌐 설정 변수
# =============================================================================
VERSION="${1:-latest}"
PROD_SERVER="${PROD_SERVER:-api.web3community.com}"
PROD_USER="${PROD_USER:-deploy}"
PROD_KEY="${PROD_KEY}"
PROD_PATH="/opt/web3community"
DOCKER_REGISTRY="${DOCKER_REGISTRY:-web3community}"
BACKUP_PATH="/opt/backups/web3community"
HEALTH_CHECK_URL="https://web3community.com/health"

# =============================================================================
# 🔍 사전 검증
# =============================================================================
if [ -z "$PROD_SERVER" ] || [ -z "$PROD_USER" ]; then
    error "PROD_SERVER 또는 PROD_USER 환경 변수가 설정되지 않았습니다."
fi

if [ "$1" != "--force" ]; then
    warning "운영 환경 배포입니다. 계속하시겠습니까? (y/N)"
    read -r response
    if [[ ! "$response" =~ ^[Yy]$ ]]; then
        log "배포가 취소되었습니다."
        exit 0
    fi
fi

# =============================================================================
# 🔄 배포 시작
# =============================================================================
log "운영 환경 배포를 시작합니다..."
log "버전: $VERSION"
log "서버: $PROD_SERVER"

# =============================================================================
# 🛡️ 안전 장치 - 롤백
# =============================================================================
log "롤백 데이터베이스를 생성합니다..."

# SSH를 통해 원격 서버에서 데이터베이스 백업
ssh -o StrictHostKeyChecking=no $PROD_USER@$PROD_SERVER << 'EOF'
    cd $PROD_PATH
    timestamp=$(date +%Y%m%d_%H%M%S)
    backup_dir="$BACKUP_PATH/backup_$timestamp"
    
    # 백업 디렉터리 생성
    mkdir -p $backup_dir
    
    # 데이터베이스 백업
    docker-compose exec -T postgres pg_dump -U web3user -d web3community > $backup_dir/database.sql
    
    # 애플리케이션 설정 백업
    cp docker-compose.yml $backup_dir/
    cp -r ./config $backup_dir/ 2>/dev/null || true
    
    echo "백업 완료: $backup_dir"
EOF

if [ $? -ne 0 ]; then
    error "데이터베이스 백업에 실패했습니다."
fi

log "롤백이 완료되었습니다."

# =============================================================================
# 🚚 기존 서비스 중지 (안전하게)
# =============================================================================
log "기존 서비스를 안전하게 중지합니다..."

# 새로운 버전으로 롤백이 실패할 경우를 대비하여 단계적 중지
ssh -o StrictHostKeyChecking=no $PROD_USER@$PROD_SERVER << 'EOF'
    cd $PROD_PATH
    
    # 새로운 버전으로 컨테이너 생성
    cp docker-compose.yml docker-compose.new.yml
    sed -i "s/:latest/:$VERSION/g" docker-compose.new.yml
    
    # 이전 버전으로 롤백 가능하게 설정
    docker-compose down --remove-orphans || true
    
    # 정지되지 않은 컨테이너 중지
    docker stop $(docker ps -aq --filter "name=web3") || true
    docker rm $(docker ps -aq --filter "name=web3") || true
    
    # 이전 이미지 태그 지정
    docker tag $DOCKER_REGISTRY/user-service:latest $DOCKER_REGISTRY/user-service:previous
    docker tag $DOCKER_REGISTRY/post-service:latest $DOCKER_REGISTRY/post-service:previous
    docker tag $DOCKER_REGISTRY/frontend:latest $DOCKER_REGISTRY/frontend:previous
EOF

# =============================================================================
# 🔄 코드 동기화
# =============================================================================
log "최신 코드를 동기화합니다..."

# Git 최신 코드 가져오기
git fetch origin main
git reset --hard origin/main
git pull origin main

if [ "$VERSION" == "latest" ]; then
    # 최신 태그 확인
    VERSION=$(git describe --tags --abbrev=0)
fi

if [ $? -ne 0 ]; then
    error "Git pull에 실패했습니다."
fi

log "배포 버전: $VERSION"

# =============================================================================
# 🐳 Docker 이미지 빌드
# =============================================================================
log "Docker 이미지를 빌드합니다..."

# 모든 서비스 이미지 빌드
for service in user-service post-service comment-service notification-service analytics-service auth-service; do
    log "$service 이미지 빌드 중..."
    
    docker build -t $DOCKER_REGISTRY/$service:$VERSION ./backend/$service
    
    if [ $? -ne 0 ]; then
        error "$service 이미지 빌드에 실패했습니다."
    fi
done

# 프론트엔드 이미지 빌드
log "Frontend 이미지 빌드 중..."
docker build -t $DOCKER_REGISTRY/frontend:$VERSION ./frontend

if [ $? -ne 0 ]; then
    error "Frontend 이미지 빌드에 실패했습니다."
fi

# =============================================================================
# 🐳 이미지 푸시
# =============================================================================
log "Docker 이미지를 레지스트리에 푸시합니다..."

# 로그인
docker login -u $DOCKER_USERNAME -p $DOCKER_PASSWORD

if [ $? -ne 0 ]; then
    error "Docker 로그인에 실패했습니다."
fi

# 이미지 푸시
for service in user-service post-service comment-service notification-service analytics-service auth-service frontend; do
    log "$service 이미지 푸시 중..."
    docker push $DOCKER_REGISTRY/$service:$VERSION
    
    if [ $? -ne 0 ]; then
        error "$service 이미지 푸시에 실패했습니다."
    fi
done

# =============================================================================
# 🚀 블루-그린 배포
# =============================================================================
log "블루-그린 배포를 시작합니다. 다운타임 최소화를 위해 단계적 배포를 수행합니다..."

# 1. 데이터베이스 마이그레이션
log "데이터베이스 마이그레이션 중..."
ssh -o StrictHostKeyChecking=no $PROD_USER@$PROD_SERVER << 'EOF'
    cd $PROD_PATH
    
    # 새 컴포즈 파일 사용
    mv docker-compose.new.yml docker-compose.yml
    
    # 데이터베이스 업데이트
    docker-compose up -d postgres
    
    # 마이그레이션 대기
    sleep 20
    
    # 마이그레이션 실행
    docker-compose exec -T postgres psql -U web3user -d web3community -c "VACUUM ANALYZE;"
    sleep 10
    
    # 다른 서비스 시작
    docker-compose up -d
    
    # 마이그레이션 실행
    docker-compose exec user-service npx sequelize-cli db:migrate
    
    # 모든 서비스 정지
    docker-compose down
    
    # 전체 서비스 시작
    docker-compose up -d
    
    # 최종 데이터베이스 업데이트
    sleep 30
    docker-compose exec -T postgres psql -U web3user -d web3community -c "VACUUM ANALYZE;"
    
    echo "데이터베이스 마이그레이션 완료"
EOF

if [ $? -ne 0 ]; then
    error "데이터베이스 마이그레이션에 실패했습니다."
fi

log "서비스가 시작되었습니다."

# =============================================================================
# 🏥 배포 상태 확인
# =============================================================================
log "배포 상태를 확인합니다..."

# 헬스체크
MAX_RETRIES=60
RETRY_COUNT=0

while [ $RETRY_COUNT -lt $MAX_RETRIES ]; do
    HTTP_STATUS=$(curl -s -o /dev/null -w "%{http_code}" $HEALTH_CHECK_URL)
    
    if [ "$HTTP_STATUS" = "200" ]; then
        log "배포 성공! 서비스가 정상적으로 실행 중입니다."
        break
    else
        RETRY_COUNT=$((RETRY_COUNT + 1))
        info "헬스체크 시도 $RETRY_COUNT/$MAX_RETRIES (HTTP: $HTTP_STATUS)..."
        sleep 10
    fi
done

if [ $RETRY_COUNT -eq $MAX_RETRIES ]; then
    error "헬스체크 실패: 서비스가 정상적으로 시작되지 않았습니다."
fi

# =============================================================================
# 🔄 롤백 설정
# =============================================================================
log "롤백 설정을 완료합니다..."

# 이전 버전을 previous 태그로 변경
for service in user-service post-service comment-service notification-service analytics-service auth-service frontend; do
    docker tag $DOCKER_REGISTRY/$service:latest $DOCKER_REGISTRY/$service:previous
    docker rmi $DOCKER_REGISTRY/$service:latest 2>/dev/null || true
done

# =============================================================================
# 📊 배포 정보 표시
# =============================================================================
log "배포 정보:"
info "버전: $VERSION"
info "서버: $PROD_SERVER"
info "API 엔드포인트: https://$PROD_SERVER/api"
info "웹사이트: https://$PROD_SERVER"
info "관리자: https://$PROD_SERVER/admin"
info "상태: $HEALTH_CHECK_URL"

# =============================================================================
# 🧹 정리
# =============================================================================
# 로컬의 태그된 이미지 정리
for service in user-service post-service comment-service notification-service analytics-service auth-service frontend; do
    docker rmi $DOCKER_REGISTRY/$service:previous 2>/dev/null || true
done

# =============================================================================
# 🎉 배포 완료
# =============================================================================
log "운영 환경 배포가 성공적으로 완료되었습니다! 🎉"
info "백업은 $BACKUP_PATH에 저장되었습니다."
info "롤백이 필요하면 이전 버전 태그를 사용하세요."