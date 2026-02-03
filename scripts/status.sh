# =============================================================================
# 🖥️ 스크립트 - 상태 확인 및 관리
# =============================================================================
# 📝 설명: 시스템 상태를 빠르게 확인하고 관리하기 위한 유틸리티 스크립트
# 🎯 목적: 개발 생산성 향상, 문제 진단 용이성, 빠른 상태 파악
# 🌟 실무 팁: 필터링 기능, 상세 정보, 실시간 모니터링 포함
# 📖 참고: kubectl 명령어 기반의 고급 활용
# =============================================================================

#!/bin/bash

set -e

# 색상 설정
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m'

# 설정
NAMESPACE="web3-community"
KUBECTL="kubectl"

# 유틸리티 함수
log_info() { echo -e "${BLUE}[INFO]${NC} $1"; }
log_success() { echo -e "${GREEN}[SUCCESS]${NC} $1"; }
log_warning() { echo -e "${YELLOW}[WARNING]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }
log_header() { echo -e "${PURPLE}=== $1 ===${NC}"; }

# =============================================================================
# 📊 전체 상태 확인
# =============================================================================
show_all_status() {
    log_header "🌐 Web3 Community Platform Status"
    echo
    
    # 네임스페이스 상태
    log_header "Namespace"
    if $KUBECTL get namespace $NAMESPACE &>/dev/null; then
        log_success "Namespace '$NAMESPACE' exists"
        $KUBECTL get namespace $NAMESPACE
    else
        log_warning "Namespace '$NAMESPACE' not found"
    fi
    echo
    
    # Pod 상태
    log_header "Pods Status"
    if $KUBECTL get pods -n $NAMESPACE &>/dev/null; then
        echo -e "${CYAN}Pod Count by Status:${NC}"
        $KUBECTL get pods -n $NAMESPACE --no-headers | awk '{print $3}' | sort | uniq -c
        
        echo -e "\n${CYAN}Pod Details:${NC}"
        $KUBECTL get pods -n $NAMESPACE -o wide
    else
        log_warning "No pods found in namespace '$NAMESPACE'"
    fi
    echo
    
    # 서비스 상태
    log_header "Services"
    if $KUBECTL get services -n $NAMESPACE &>/dev/null; then
        $KUBECTL get services -n $NAMESPACE
    else
        log_warning "No services found"
    fi
    echo
    
    # 디플로이먼트 상태
    log_header "Deployments"
    if $KUBECTL get deployments -n $NAMESPACE &>/dev/null; then
        $KUBECTL get deployments -n $NAMESPACE
    else
        log_warning "No deployments found"
    fi
    echo
    
    # PVC 상태
    log_header "Persistent Volumes"
    if $KUBECTL get pvc -n $NAMESPACE &>/dev/null; then
        $KUBECTL get pvc -n $NAMESPACE
    else
        log_warning "No PVCs found"
    fi
    echo
    
    # Ingress 상태
    log_header "Ingress"
    if $KUBECTL get ingress -n $NAMESPACE &>/dev/null; then
        $KUBECTL get ingress -n $NAMESPACE
    else
        log_warning "No Ingress found"
    fi
    echo
    
    # HPA 상태
    log_header "Horizontal Pod Autoscalers"
    if $KUBECTL get hpa -n $NAMESPACE &>/dev/null; then
        $KUBECTL get hpa -n $NAMESPACE
    else
        log_warning "No HPAs found"
    fi
}

# =============================================================================
# 🔍 Pod 상세 상태 확인
# =============================================================================
check_pod_details() {
    local pod_pattern=${1:-""}
    
    log_header "🔍 Pod Details Check"
    
    if [[ -n "$pod_pattern" ]]; then
        log_info "Checking pods matching: $pod_pattern"
        $KUBECTL get pods -n $NAMESPACE -l app="$pod_pattern" -o wide
    else
        $KUBECTL get pods -n $NAMESPACE -o wide
    fi
    
    echo
    log_info "Pod Events (Last 10):"
    $KUBECTL get events -n $NAMESPACE --sort-by='.lastTimestamp' | tail -10
    
    echo
    log_info "Resource Usage:"
    if command -v kubectl top &>/dev/null; then
        $KUBECTL top pods -n $NAMESPACE
    else
        log_warning "kubectl top not available. Install metrics-server."
    fi
}

# =============================================================================
# 🚨 문제 진단
# =============================================================================
diagnose_issues() {
    log_header "🚨 Issue Diagnosis"
    
    # Pending Pods 확인
    log_info "Checking for Pending pods..."
    local pending_pods=$($KUBECTL get pods -n $NAMESPACE --field-selector=status.phase=Pending --no-headers 2>/dev/null | wc -l)
    if [[ $pending_pods -gt 0 ]]; then
        log_warning "Found $pending_pods pending pods:"
        $KUBECTL get pods -n $NAMESPACE --field-selector=status.phase=Pending
        echo
        log_info "Pending pod details:"
        $KUBECTL describe pods -n $NAMESPACE --field-selector=status.phase=Pending | grep -A 5 "Events:"
    else
        log_success "No pending pods found"
    fi
    echo
    
    # Failed Pods 확인
    log_info "Checking for Failed pods..."
    local failed_pods=$($KUBECTL get pods -n $NAMESPACE --field-selector=status.phase=Failed --no-headers 2>/dev/null | wc -l)
    if [[ $failed_pods -gt 0 ]]; then
        log_warning "Found $failed_pods failed pods:"
        $KUBECTL get pods -n $NAMESPACE --field-selector=status.phase=Failed
    else
        log_success "No failed pods found"
    fi
    echo
    
    # CrashLoopBackOff 확인
    log_info "Checking for CrashLoopBackOff pods..."
    local crashloop_pods=$($KUBECTL get pods -n $NAMESPACE --no-headers 2>/dev/null | grep "CrashLoopBackOff" | wc -l)
    if [[ $crashloop_pods -gt 0 ]]; then
        log_warning "Found $crashloop_pods CrashLoopBackOff pods:"
        $KUBECTL get pods -n $NAMESPACE | grep "CrashLoopBackOff"
    else
        log_success "No CrashLoopBackOff pods found"
    fi
    echo
    
    # PVC 상태 확인
    log_info "Checking PVC status..."
    local pending_pvcs=$($KUBECTL get pvc -n $NAMESPACE --no-headers 2>/dev/null | grep "Pending" | wc -l)
    if [[ $pending_pvcs -gt 0 ]]; then
        log_warning "Found $pending_pvcs pending PVCs:"
        $KUBECTL get pvc -n $NAMESPACE | grep "Pending"
    else
        log_success "All PVCs are bound"
    fi
    echo
    
    # 서비스 엔드포인트 확인
    log_info "Checking service endpoints..."
    local services_without_endpoints=$($KUBECTL get endpoints -n $AMESPACE --no-headers 2>/dev/null | grep "<none>" | wc -l)
    if [[ $services_without_endpoints -gt 0 ]]; then
        log_warning "Found $services_without_endpoints services without endpoints:"
        $KUBECTL get endpoints -n $NAMESPACE | grep "<none>"
    else
        log_success "All services have endpoints"
    fi
}

# =============================================================================
# 📊 리소스 사용량 모니터링
# =============================================================================
monitor_resources() {
    log_header "📊 Resource Monitoring"
    
    # Pod 리소스 사용량
    if command -v kubectl top &>/dev/null; then
        log_info "Pod Resource Usage:"
        $KUBECTL top pods -n $NAMESPACE --containers=true
        echo
        
        log_info "Node Resource Usage:"
        $KUBECTL top nodes
        echo
    else
        log_warning "kubectl top not available. Install metrics-server:"
        echo "Minikube: minikube addons enable metrics-server"
        echo "Docker Desktop: Enabled by default"
        echo
    fi
    
    # 할당된 리소스
    log_info "Allocated Resources:"
    $KUBECTL describe namespace $NAMESPACE | grep -A 10 "Resource Quotas" || echo "No resource quotas set"
    echo
    
    # 리소스 리밋 확인
    log_info "Pod Resource Limits:"
    $KUBECTL get pods -n $NAMESPACE -o jsonpath='{range .items[*]}{.metadata.name}{"\t"}{.spec.containers[0].resources.limits}{"\n"}{end}'
}

# =============================================================================
# 🔗 네트워크 연결 테스트
# =============================================================================
test_connectivity() {
    log_header "🔗 Network Connectivity Test"
    
    # 서비스 DNS 확인
    log_info "Testing service DNS resolution..."
    services=("$NAMESPACE-api-gateway-service" "$NAMESPACE-user-service-service" "$NAMESPACE-post-service-service" "$NAMESPACE-mysql-service" "$NAMESPACE-mongodb-service" "$NAMESPACE-redis-service" "$NAMESPACE-kafka-service")
    
    for service in "${services[@]}"; do
        if $KUBECTL run dns-test-$RANDOM --image=busybox --rm -i --restart=Never -- nslookup $service.$NAMESPACE.svc.cluster.local &>/dev/null; then
            log_success "✓ $service"
        else
            log_warning "✗ $service"
        fi
    done
    
    echo
    
    # Ingress 접속 테스트
    log_info "Testing Ingress connectivity..."
    if $KUBECTL get ingress -n $NAMESPACE &>/dev/null; then
        local ingress_host=$($KUBECTL get ingress -n $NAMESPACE -o jsonpath='{.items[0].spec.rules[0].host}' 2>/dev/null)
        if [[ -n "$ingress_host" ]]; then
            log_info "Ingress host: $ingress_host"
            log_info "Test command: curl -I http://$ingress_host"
        else
            log_warning "No ingress host found"
        fi
    else
        log_warning "No ingress configured"
    fi
    
    echo
    
    # Pod 간 통신 테스트
    log_info "Testing inter-pod communication..."
    if $KUBECTL get pod -n $NAMESPACE -l app=api-gateway &>/dev/null; then
        $KUBECTL run connectivity-test-$RANDOM --image=curlimages/curl --rm -i --restart=Never -- \
            curl -s http://api-gateway-service:8080/actuator/health || log_warning "API Gateway not reachable"
    fi
}

# =============================================================================
# 📈 실시간 모니터링
# =============================================================================
watch_status() {
    log_header "📈 Real-time Monitoring (Press Ctrl+C to exit)"
    
    # watch 명령어 사용 가능 확인
    if command -v watch &>/dev/null; then
        watch -n 5 "$KUBECTL get pods,services,deployments -n $NAMESPACE"
    else
        log_info "watch command not available. Monitoring manually..."
        while true; do
            clear
            show_all_status
            sleep 5
        done
    fi
}

# =============================================================================
# 📋 도움말
# =============================================================================
show_help() {
    cat << EOF
Web3 Community Status Script

Usage: $0 [COMMAND] [OPTIONS]

Commands:
  all                     Show all status (default)
  pods [pattern]          Show pod details (optional pattern filter)
  diagnose               Diagnose common issues
  resources              Monitor resource usage
  network                Test network connectivity
  watch                  Real-time monitoring
  help                   Show this help

Examples:
  $0                      # Show all status
  $0 pods                 # Show pod details
  $0 pods api-gateway     # Show api-gateway pods only
  $0 diagnose             # Diagnose issues
  $0 resources            # Monitor resource usage
  $0 network              # Test connectivity
  $0 watch                # Real-time monitoring

Filter patterns:
  - api-gateway, user-service, post-service, comment-service, auth-service, notification-service
  - mysql, mongodb, redis, kafka, zookeeper
  - frontend, backend, database
EOF
}

# =============================================================================
# 🎯 메인 실행 로직
# =============================================================================
main() {
    case "${1:-all}" in
        "all"|"")
            show_all_status
            ;;
        "pods")
            check_pod_details "$2"
            ;;
        "diagnose")
            diagnose_issues
            ;;
        "resources")
            monitor_resources
            ;;
        "network")
            test_connectivity
            ;;
        "watch")
            watch_status
            ;;
        "help"|"--help"|"-h")
            show_help
            ;;
        *)
            log_error "Unknown command: $1"
            show_help
            exit 1
            ;;
    esac
}

# 스크립트 실행
main "$@"