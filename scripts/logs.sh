# =============================================================================
# 📝 로그 확인 스크립트
# =============================================================================
# 📝 설명: 여러 서비스의 로그를 쉽게 확인하고 필터링하는 스크립트
# 🎯 목적: 문제 진단, 실시간 모니터링, 로그 분석 용이성
# 🌟 실무 팁: 여러 서비스 동시 모니터링, 필터링, 색상 출력
# 📖 참고: kubectl logs 명령어 고급 활용
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

# 서비스 목록
declare -A SERVICES=(
    ["frontend"]="frontend"
    ["api-gateway"]="api-gateway"
    ["user-service"]="user-service"
    ["post-service"]="post-service"
    ["comment-service"]="comment-service"
    ["auth-service"]="auth-service"
    ["notification-service"]="notification-service"
    ["mysql"]="mysql"
    ["mongodb"]="mongodb"
    ["redis"]="redis"
    ["kafka"]="kafka"
    ["zookeeper"]="zookeeper"
)

declare -A LOG_LABELS=(
    ["frontend"]="app=frontend"
    ["api-gateway"]="app=api-gateway"
    ["user-service"]="app=user-service"
    ["post-service"]="app=post-service"
    ["comment-service"]="app=comment-service"
    ["auth-service"]="app=auth-service"
    ["notification-service"]="app=notification-service"
    ["mysql"]="component=database,database=mysql"
    ["mongodb"]="component=database,database=mongodb"
    ["redis"]="component=database,database=redis"
    ["kafka"]="component=database,database=kafka"
    ["zookeeper"]="component=database,database=zookeeper"
)

# =============================================================================
# 📝 단일 서비스 로그 확인
# =============================================================================
show_service_logs() {
    local service_name=$1
    local follow=${2:-false}
    local tail_lines=${3:-100}
    local filter=${4:-""}
    
    if [[ -z "$service_name" ]]; then
        log_error "Service name is required"
        show_available_services
        return 1
    fi
    
    if [[ -z "${SERVICES[$service_name]}" ]]; then
        log_error "Unknown service: $service_name"
        show_available_services
        return 1
    fi
    
    local label_selector="${LOG_LABELS[$service_name]}"
    local pod_name=$($KUBECTL get pods -n $NAMESPACE -l "$label_selector" -o jsonpath='{.items[0].metadata.name}' 2>/dev/null)
    
    if [[ -z "$pod_name" ]]; then
        log_error "No running pod found for service: $service_name"
        return 1
    fi
    
    log_header "📝 Logs for $service_name (Pod: $pod_name)"
    
    local log_command="$KUBECTL logs -n $NAMESPACE $pod_name"
    
    # 옵션 추가
    if [[ "$follow" == "true" ]]; then
        log_command="$log_command -f"
    fi
    
    log_command="$log_command --tail=$tail_lines"
    
    # 필터 적용
    if [[ -n "$filter" ]]; then
        log_info "Filter: $filter"
        log_command="$log_command | grep -E '$filter'"
    fi
    
    log_info "Command: $log_command"
    echo
    
    eval $log_command
}

# =============================================================================
# 📝 여러 서비스 로그 동시 확인
# =============================================================================
show_multiple_logs() {
    local follow=${1:-false}
    local tail_lines=${2:-50}
    shift 2
    local services=("$@")
    
    if [[ ${#services[@]} -eq 0 ]]; then
        log_error "At least one service name is required"
        show_available_services
        return 1
    fi
    
    log_header "📝 Multiple Service Logs"
    log_info "Services: ${services[*]}"
    log_info "Follow: $follow, Tail: $tail_lines lines"
    echo
    
    # GNU screen 사용 가능한지 확인
    if command -v screen &>/dev/null && [[ "$follow" == "true" ]]; then
        log_info "Using screen for multi-view"
        
        # screen 세션 생성
        screen_session="web3-logs-$(date +%s)"
        screen -dmS "$screen_session"
        
        # 각 서비스에 대해 창 생성
        local window_num=0
        for service in "${services[@]}"; do
            if [[ -n "${SERVICES[$service]}" ]]; then
                local label_selector="${LOG_LABELS[$service]}"
                local pod_name=$($KUBECTL get pods -n $NAMESPACE -l "$label_selector" -o jsonpath='{.items[0].metadata.name}' 2>/dev/null)
                
                if [[ -n "$pod_name" ]]; then
                    screen -S "$screen_session" -X screen -t "$service" bash -c "$KUBECTL logs -f -n $NAMESPACE --tail=$tail_lines $pod_name"
                    ((window_num++))
                fi
            fi
        done
        
        log_success "Screen session '$screen_session' created. Attach with: screen -r $screen_session"
        
    else
        # 순차적으로 로그 출력
        for service in "${services[@]}"; do
            if [[ -n "${SERVICES[$service]}" ]]; then
                echo -e "${CYAN}=== $service ===${NC}"
                show_service_logs "$service" "false" "$tail_lines"
                echo
                echo "Press Enter to continue (or 'q' to quit)..."
                read -r input
                if [[ "$input" == "q" ]]; then
                    break
                fi
            else
                log_warning "Unknown service: $service"
            fi
        done
    fi
}

# =============================================================================
# 🔍 로그 검색
# =============================================================================
search_logs() {
    local search_term=$1
    local service=${2:-""}
    local follow=${3:-false}
    local tail_lines=${4:-200}
    
    if [[ -z "$search_term" ]]; then
        log_error "Search term is required"
        echo "Usage: $0 search <search_term> [service] [follow] [tail_lines]"
        return 1
    fi
    
    log_header "🔍 Searching logs for: $search_term"
    
    if [[ -n "$service" && -n "${SERVICES[$service]}" ]]; then
        # 특정 서비스 검색
        show_service_logs "$service" "$follow" "$tail_lines" "$search_term"
    else
        # 모든 서비스 검색
        log_info "Searching all services..."
        local label_selector=""
        local log_command="$KUBECTL logs -n $NAMESPACE -l component --all-containers=true"
        
        if [[ "$follow" == "true" ]]; then
            log_command="$log_command -f"
        fi
        
        log_command="$log_command --tail=$tail_lines | grep -E --color=always '$search_term'"
        
        eval $log_command
    fi
}

# =============================================================================
# 📊 로그 통계
# =============================================================================
show_log_stats() {
    log_header "📊 Log Statistics"
    
    echo "Service pod counts:"
    for service in "${!SERVICES[@]}"; do
        local label_selector="${LOG_LABELS[$service]}"
        local pod_count=$($KUBECTL get pods -n $NAMESPACE -l "$label_selector" --no-headers 2>/dev/null | wc -l)
        echo "  $service: $pod_count pod(s)"
    done
    
    echo
    echo "Error counts in recent logs:"
    for service in "${!SERVICES[@]}"; do
        local label_selector="${LOG_LABELS[$service]}"
        local pod_name=$($KUBECTL get pods -n $NAMESPACE -l "$label_selector" -o jsonpath='{.items[0].metadata.name}' 2>/dev/null)
        
        if [[ -n "$pod_name" ]]; then
            local error_count=$($KUBECTL logs -n $NAMESPACE "$pod_name" --tail=100 2>/dev/null | grep -i -c "error\|exception\|failed" || echo "0")
            echo "  $service: $error_count error(s)"
        fi
    done
}

# =============================================================================
# 📋 사용 가능한 서비스 목록
# =============================================================================
show_available_services() {
    log_header "📋 Available Services"
    
    echo "Frontend:"
    echo "  frontend"
    
    echo
    echo "Backend Services:"
    echo "  api-gateway"
    echo "  user-service"
    echo "  post-service"
    echo "  comment-service"
    echo "  auth-service"
    echo "  notification-service"
    
    echo
    echo "Databases:"
    echo "  mysql"
    echo "  mongodb"
    echo "  redis"
    echo "  kafka"
    echo "  zookeeper"
    
    echo
    echo "Special:"
    echo "  all     - All services"
    echo "  backend - All backend services"
    echo "  db      - All databases"
}

# =============================================================================
# 🎯 메인 실행 로직
# =============================================================================
main() {
    local command=${1:-"help"}
    
    case "$command" in
        "show"|"")
            show_service_logs "$2" "$3" "$4" "$5"
            ;;
        "follow")
            show_service_logs "$2" "true" "$4" "$5"
            ;;
        "multi")
            show_multiple_logs "$2" "$3" "${@:4}"
            ;;
        "search")
            search_logs "$2" "$3" "$4" "$5"
            ;;
        "stats")
            show_log_stats
            ;;
        "all")
            show_multiple_logs "false" "50" frontend api-gateway user-service post-service comment-service auth-service notification-service
            ;;
        "backend")
            show_multiple_logs "$2" "50" api-gateway user-service post-service comment-service auth-service notification-service
            ;;
        "db")
            show_multiple_logs "$2" "50" mysql mongodb redis kafka zookeeper
            ;;
        "help"|"--help"|"-h")
            cat << EOF
Web3 Community Log Viewer Script

Usage: $0 [COMMAND] [OPTIONS]

Commands:
  show [service] [follow] [tail] [filter]     Show logs for specific service
  follow [service] [tail] [filter]          Follow logs in real-time
  multi [follow] [tail] service1 service2... Show logs for multiple services
  search <term> [service] [follow] [tail]   Search logs for specific term
  stats                                      Show log statistics
  all                                        Show all service logs
  backend                                    Show all backend service logs
  db                                         Show all database logs
  help                                       Show this help

Examples:
  $0 show api-gateway                         # Show api-gateway logs
  $0 follow api-gateway 200                   # Follow api-gateway with 200 lines
  $0 show api-gateway false 100 "error"      # Show api-gateway errors
  $0 search "database" mysql                   # Search for database errors in mysql
  $0 multi false 100 api-gateway user-service # Show multiple services
  $0 follow mysql                             # Follow mysql logs in real-time
  $0 all                                      # Show all services
  $0 stats                                    # Show log statistics

Services:
  frontend, api-gateway, user-service, post-service, comment-service, 
  auth-service, notification-service, mysql, mongodb, redis, kafka, zookeeper

Tips:
  - Use 'follow' for real-time monitoring
  - Use 'multi' with screen for simultaneous views
  - Use 'search' to find specific patterns
  - Use 'stats' to quickly check for errors
EOF
            ;;
        *)
            log_error "Unknown command: $command"
            show_help
            exit 1
            ;;
    esac
}

# 스크립트 실행
main "$@"