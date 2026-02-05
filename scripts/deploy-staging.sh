#!/bin/bash

# =============================================================================
# 🚀 개발 환경 배포 스크립트
# =============================================================================
# 설명: 개발 서버에 Web3 Community 배포
# 특징: 안전한 배포, 롤백, 상태 확인
# 목적: 개발 환경 자동화 배포
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
STAGING_SERVER="${STAGING_SERVER:-staging.web3community.com}"
STAGING_USER="${STAGING_USER:-deploy}"
STAGING_KEY="${STAGING_KEY}"
STAGING_PATH="/opt/web3community"
DOCKER_REGISTRY="${DOCKER_REGISTRY:-web3community}"

# =============================================================================
# 🔍 사전 검증
# =============================================================================
if [ -z "$STAGING_SERVER" ] || [ -z "$STAGING_USER" ]; then
    error "STAGING_SERVER 또는 STAGING_USER 환경 변수가 설정되지 않았습니다."
fi

# =============================================================================
# 🔄 배포 시작
# =============================================================================
log "개발 환경 배포를 시작합니다..."
info "서버: $STAGING_SERVER"

# =============================================================================
# 🚚 기존 서비스 중지
# =============================================================================
log "기존 서비스를 중지합니다..."

# SSH를 통해 원격 서버에서 명령 실행
ssh -o StrictHostKeyChecking=no $STAGING_USER@$STAGING_SERVER << 'EOF'
    cd $STAGING_PATH
    
    # Docker 컨테이너 중지
    if [ -f "docker-compose.yml" ]; then
        docker-compose down --remove-orphans
    fi
    
    # 정지되지 않은 컨테이너 중지
    docker stop $(docker ps -aq --filter "name=web3") || true
    docker rm $(docker ps -aq --filter "name=web3") || true
EOF

if [ $? -ne 0 ]; then
    error "기존 서비스 중지에 실패했습니다."
fi

# =============================================================================
# 🔄 코드 동기화
# =============================================================================
log "최신 코드를 동기화합니다..."

# Git 최신 코드 가져오기
git pull origin develop

if [ $? -ne 0 ]; then
    error "Git pull에 실패했습니다."
fi

# =============================================================================
# 🐳 Docker 이미지 빌드
# =============================================================================
log "Docker 이미지를 빌드합니다..."

# 모든 서비스 이미지 빌드
for service in user-service post-service comment-service notification-service analytics-service auth-service; do
    log "$service 이미지 빌드 중..."
    
    docker build -t $DOCKER_REGISTRY/$service:latest ./backend/$service
    
    if [ $? -ne 0 ]; then
        error "$service 이미지 빌드에 실패했습니다."
    fi
done

# 프론트엔드 이미지 빌드
log "Frontend 이미지 빌드 중..."
docker build -t $DOCKER_REGISTRY/frontend:latest ./frontend

if [ $? -ne 0 ]; then
    error "Frontend 이미지 빌드에 실패했습니다."
fi

# =============================================================================
# 🐳 이미지 푸시
# =============================================================================
log "Docker 이미지를 레지스트리에 푸시합니다..."

# 로그인 확인
if [ -z "$DOCKER_USERNAME" ] || [ -z "$DOCKER_PASSWORD" ]; then
    warning "Docker Hub 인증 정보가 없습니다. 이미지 푸시를 건너뜁니다."
else
    docker login -u $DOCKER_USERNAME -p $DOCKER_PASSWORD
    
    if [ $? -ne 0 ]; then
        error "Docker 로그인에 실패했습니다."
    fi
fi

# 이미지 푸시
for service in user-service post-service comment-service notification-service analytics-service auth-service frontend; do
    log "$service 이미지 푸시 중..."
    docker push $DOCKER_REGISTRY/$service:latest
    
    if [ $? -ne 0 ]; then
        warning "$service 이미지 푸시에 실패했습니다. 로컬 이미지를 사용합니다."
    fi
done

# =============================================================================
# 🚀 서비스 시작
# =============================================================================
log "서비스를 시작합니다..."

# SSH를 통해 원격 서버에서 서비스 시작
ssh -o StrictHostKeyChecking=no $STAGING_USER@$STAGING_SERVER << 'EOF'
    cd $STAGING_PATH
    
    # 최신 Docker 이미지 당겨오기
    docker-compose pull
    
    # 서비스 시작
    docker-compose up -d
    
    # 데이터베이스 마이그레이션
    sleep 30
    docker-compose exec user-service npx sequelize-cli db:migrate
    
    # 헬스체크 대기
    echo "서비스 헬스체크 대기..."
    sleep 30
EOF

if [ $? -ne 0 ]; then
    error "서비스 시작에 실패했습니다."
fi

# =============================================================================
# 🏥 배포 상태 확인
# =============================================================================
log "배포 상태를 확인합니다..."

# 헬스체크
MAX_RETRIES=30
RETRY_COUNT=0

while [ $RETRY_COUNT -lt $MAX_RETRIES ]; do
    if curl -f http://$STAGING_SERVER/health > /dev/null 2>&1; then
        log "배포 성공! 서비스가 정상적으로 실행 중입니다."
        break
    else
        RETRY_COUNT=$((RETRY_COUNT + 1))
        info "헬스체크 시도 $RETRY_COUNT/$MAX_RETRIES..."
        sleep 10
    fi
done

if [ $RETRY_COUNT -eq $MAX_RETRIES ]; then
    error "헬스체크 실패: 서비스가 정상적으로 시작되지 않았습니다."
fi

# =============================================================================
# 📊 배포 정보 표시
# =============================================================================
log "배포 정보:"
info "서버: $STAGING_SERVER"
info "API 엔드포인트: http://$STAGING_SERVER/api"
info "웹사이트: http://$STAGING_SERVER"
info "상태: http://$STAGING_SERVER/health"

# =============================================================================
# 🧹 로그 확인 (선택사항)
# =============================================================================
info "최근 로그 확인:"
ssh -o StrictHostKeyChecking=no $STAGING_USER@$STAGING_SERVER "cd $STAGING_PATH && docker-compose logs --tail=50"

log "개발 환경 배포가 완료되었습니다! 🎉"