# =============================================================================
# 🛠️ 자동화 스크립트 - 전체 배포 관리
# =============================================================================
# 📝 설명: Web3 Community Platform 전체 배포를 위한 자동화 스크립트
# 🎯 목적: One-Command 배포, 개발 생산성 향상, 실수 방지
# 🌟 실무 팁: 순차적 배포, 롤백 기능, 상태 확인 포함
# ⚠️  주의: Minikube/Docker Desktop 사전 설치 필요
# 📖 참고: https://kubernetes.io/docs/reference/generated/kubectl/kubectl-commands
# =============================================================================

#!/bin/bash

# =============================================================================
# 🚀 스크립트 설정 및 초기화
# =============================================================================
set -e  # 오류 발생 시 즉시 종료
set -u  # 미선언 변수 사용 시 종료
set -o pipefail  # 파이프라인 오류 감지

# 색상 출력 설정
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# 프로젝트 설정
PROJECT_NAME="web3-community"
NAMESPACE="web3-community"
KUBECTL="kubectl"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

# =============================================================================
# 🔧 유틸리티 함수들
# =============================================================================

# 로그 출력 함수
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

log_header() {
    echo -e "${PURPLE}================================${NC}"
    echo -e "${PURPLE}$1${NC}"
    echo -e "${PURPLE}================================${NC}"
}

# 상태 확인 함수
check_status() {
    local resource_type=$1
    local resource_name=$2
    local expected_status=$3
    
    log_info "Checking $resource_type/$resource_name status..."
    
    if $KUBECTL get $resource_type $resource_name -n $NAMESPACE &>/dev/null; then
        local current_status=$($KUBECTL get $resource_type $resource_name -n $NAMESPACE -o jsonpath='{.status.phase}' 2>/dev/null || echo "Active")
        
        if [[ "$current_status" == "$expected_status" ]] || [[ -z "$expected_status" ]]; then
            log_success "$resource_type/$resource_name is ready"
            return 0
        else
            log_warning "$resource_type/$resource_name status: $current_status"
            return 1
        fi
    else
        log_error "$resource_type/$resource_name not found"
        return 1
    fi
}

# 대기 함수
wait_for_ready() {
    local resource_type=$1
    local resource_name=$2
    local timeout=${3:-300}
    local interval=${4:-5}
    
    log_info "Waiting for $resource_type/$resource_name to be ready (timeout: ${timeout}s)..."
    
    local elapsed=0
    while [[ $elapsed -lt $timeout ]]; do
        if check_status "$resource_type" "$resource_name"; then
            log_success "$resource_type/$resource_name is ready!"
            return 0
        fi
        
        sleep $interval
        elapsed=$((elapsed + interval))
        echo -n "."
    done
    
    log_error "$resource_type/$resource_name failed to become ready within ${timeout}s"
    return 1
}

# =============================================================================
# 🔍 사전 환경 확인
# =============================================================================
check_prerequisites() {
    log_header "🔍 Checking Prerequisites"
    
    # kubectl 설치 확인
    if ! command -v kubectl &> /dev/null; then
        log_error "kubectl is not installed. Please install kubectl first."
        exit 1
    fi
    log_success "kubectl found: $(kubectl version --client --short)"
    
    # Docker 설치 확인
    if ! command -v docker &> /dev/null; then
        log_error "Docker is not installed. Please install Docker first."
        exit 1
    fi
    log_success "Docker found: $(docker --version)"
    
    # Minikube/Docker Desktop 확인
    if kubectl cluster-info &> /dev/null; then
        log_success "Kubernetes cluster is running"
        kubectl cluster-info
    else
        log_error "Kubernetes cluster is not running. Please start Minikube or Docker Desktop."
        exit 1
    fi
    
    # 프로젝트 디렉토리 확인
    if [[ ! -d "$PROJECT_DIR/k8s" ]]; then
        log_error "Project k8s directory not found at $PROJECT_DIR/k8s"
        exit 1
    fi
    log_success "Project directory found: $PROJECT_DIR"
}

# =============================================================================
# 🚀 전체 배포 함수
# =============================================================================

deploy_all() {
    log_header "🚀 Starting Full Deployment"
    
    cd "$PROJECT_DIR/k8s"
    
    # 1단계: 네임스페이스 생성
    log_info "Step 1: Creating namespace"
    $KUBECTL apply -f 01-namespace.yaml
    wait_for_ready "namespace" "$NAMESPACE"
    
    # 2단계: ConfigMaps 적용
    log_info "Step 2: Applying ConfigMaps"
    $KUBECTL apply -f 02-configmaps/
    
    # 3단계: Secrets 적용
    log_info "Step 3: Applying Secrets"
    $KUBECTL apply -f 03-secrets/
    
    # 4단계: 데이터베이스 배포
    log_info "Step 4: Deploying databases"
    $KUBECTL apply -f 04-storage/
    
    # 데이터베이스 준비 대기
    log_info "Waiting for databases to be ready..."
    wait_for_ready "pod" -l component=database "Running" 600
    
    # 5단계: 애플리케이션 배포
    log_info "Step 5: Deploying applications"
    $KUBECTL apply -f 05-applications/
    
    # 애플리케이션 준비 대기
    log_info "Waiting for applications to be ready..."
    wait_for_ready "pod" -l deployment-type=microservice "Running" 300
    
    # 6단계: 네트워킹 설정
    log_info "Step 6: Configuring networking"
    $KUBECTL apply -f 06-networking/
    
    log_success "🎉 Full deployment completed successfully!"
    
    # 최종 상태 확인
    show_status
}

# =============================================================================
# 🧹 전체 삭제 함수
# =============================================================================

delete_all() {
    log_header "🧹 Starting Full Cleanup"
    
    cd "$PROJECT_DIR/k8s"
    
    # 네트워킹 먼저 삭제
    log_info "Deleting networking..."
    $KUBECTL delete -f 06-networking/ --ignore-not-found=true
    
    # 애플리케이션 삭제
    log_info "Deleting applications..."
    $KUBECTL delete -f 05-applications/ --ignore-not-found=true
    
    # 데이터베이스 삭제
    log_info "Deleting databases..."
    $KUBECTL delete -f 04-storage/ --ignore-not-found=true
    
    # ConfigMaps/Secrets 삭제
    log_info "Deleting configurations..."
    $KUBECTL delete -f 02-configmaps/ --ignore-not-found=true
    $KUBECTL delete -f 03-secrets/ --ignore-not-found=true
    
    # 네임스페이스 삭제 (마지막)
    log_info "Deleting namespace..."
    $KUBECTL delete -f 01-namespace.yaml --ignore-not-found=true
    
    log_success "🧹 Full cleanup completed!"
}

# =============================================================================
# 📊 상태 확인 함수
# =============================================================================

show_status() {
    log_header "📊 Current Status"
    
    echo
    log_info "=== Namespace ==="
    $KUBECTL get namespace $NAMESPACE
    
    echo
    log_info "=== Pods ==="
    $KUBECTL get pods -n $NAMESPACE -o wide
    
    echo
    log_info "=== Services ==="
    $KUBECTL get services -n $NAMESPACE
    
    echo
    log_info "=== Deployments ==="
    $KUBECTL get deployments -n $NAMESPACE
    
    echo
    log_info "=== Persistent Volumes ==="
    $KUBECTL get pvc -n $NAMESPACE
    
    echo
    log_info "=== Ingress ==="
    $KUBECTL get ingress -n $NAMESPACE
    
    echo
    log_info "=== HPAs ==="
    $KUBECTL get hpa -n $NAMESPACE
}

# =============================================================================
# 🔄 재시작 함수
# =============================================================================

restart_service() {
    local service_name=$1
    
    if [[ -z "$service_name" ]]; then
        log_error "Please specify a service name to restart"
        log_info "Available services:"
        $KUBECTL get deployments -n $NAMESPACE -o jsonpath='{.items[*].metadata.name}' | tr ' ' '\n'
        exit 1
    fi
    
    log_info "Restarting service: $service_name"
    $KUBECTL rollout restart deployment/$service_name-deployment -n $NAMESPACE
    $KUBECTL rollout status deployment/$service_name-deployment -n $NAMESPACE
    log_success "Service $service_name restarted successfully!"
}

restart_all() {
    log_header "🔄 Restarting All Services"
    
    for service in api-gateway user-service post-service comment-service auth-service notification-service; do
        restart_service $service
    done
}

# =============================================================================
# 📝 로그 확인 함수
# =============================================================================

show_logs() {
    local pod_name=$1
    local follow=${2:-false}
    
    if [[ -z "$pod_name" ]]; then
        log_info "Available pods:"
        $KUBECTL get pods -n $NAMESPACE -o jsonpath='{.items[*].metadata.name}' | tr ' ' '\n'
        exit 0
    fi
    
    if [[ "$follow" == "true" ]]; then
        $KUBECTL logs -f deployment/$pod_name-deployment -n $NAMESPACE
    else
        $KUBECTL logs deployment/$pod_name-deployment -n $NAMESPACE --tail=50
    fi
}

# =============================================================================
# 🐛 디버깅 함수
# =============================================================================

debug_pod() {
    local pod_name=$1
    
    if [[ -z "$pod_name" ]]; then
        log_error "Please specify a pod name to debug"
        exit 1
    fi
    
    log_info "Opening shell in pod: $pod_name"
    $KUBECTL exec -it deployment/$pod_name-deployment -n $NAMESPACE -- /bin/bash
}

# =============================================================================
# 🎯 메인 실행 로직
# =============================================================================

main() {
    # 스크립트 인자 파싱
    case "${1:-}" in
        "deploy"|"")
            check_prerequisites
            deploy_all
            ;;
        "delete"|"clean")
            delete_all
            ;;
        "status")
            show_status
            ;;
        "restart")
            restart_service "$2"
            ;;
        "restart-all")
            restart_all
            ;;
        "logs")
            show_logs "$2" "$3"
            ;;
        "logs-follow")
            show_logs "$2" "true"
            ;;
        "debug")
            debug_pod "$2"
            ;;
        "help"|"--help"|"-h")
            cat << EOF
Web3 Community Deployment Script

Usage: $0 [COMMAND] [OPTIONS]

Commands:
  deploy          Deploy all services (default)
  delete          Delete all services
  status          Show current status
  restart <svc>  Restart specific service
  restart-all     Restart all services
  logs <svc>      Show logs for specific service
  logs-follow <svc>  Follow logs for specific service
  debug <pod>     Open shell in specific pod
  help            Show this help message

Examples:
  $0 deploy                 # Deploy all services
  $0 restart api-gateway     # Restart API Gateway
  $0 logs user-service       # Show user service logs
  $0 status                 # Show all status

Services:
  api-gateway, user-service, post-service, comment-service, auth-service, notification-service
EOF
            ;;
        *)
            log_error "Unknown command: $1"
            log_info "Use '$0 help' to see available commands"
            exit 1
            ;;
    esac
}

# 스크립트 실행
main "$@"