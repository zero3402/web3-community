# 🚀 빠른 시작 가이드

이 문서는 Web3 Community Platform을 로컬 환경에서 빠르게 실행하는 방법을 설명합니다.

## 📋 사전 준비물

### 시스템 요구사항
- **운영체제**: Windows 10/11 (64bit)
- **메모리**: 최소 8GB RAM (권장 16GB)
- **디스크**: 최소 20GB 여유 공간
- **CPU**: 최소 4코어

### 필수 소프트웨어
1. **Docker Desktop** (v4.0+)
   - [Docker Desktop for Windows](https://www.docker.com/products/docker-desktop/)
   - WSL2 지원 필요
   - Kubernetes 활성화 필요

2. **kubectl** (v1.24+)
   ```powershell
   # Chocolatey로 설치 (권장)
   choco install kubernetes-cli
   
   # 또는 직접 다운로드
   curl.exe -LO "https://dl.k8s.io/release/v1.28.0/bin/windows/amd64/kubectl.exe"
   ```

3. **Git** (v2.0+)
   ```powershell
   choco install git
   ```

## ⚡ 5분 만에 시작하기

### 1단계: 환경 확인 (1분)

```bash
# Docker Desktop 확인
docker --version
kubectl version --client

# Kubernetes 클러스터 확인
kubectl cluster-info

# Docker Desktop Kubernetes 활성화 확인
kubectl get nodes
```

### 2단계: 프로젝트 클론 (1분)

```bash
git clone <repository-url>
cd web3-community
```

### 3단계: 전체 배포 (3분)

```bash
# 한번에 전체 배포
./scripts/deploy.sh deploy
```

### 4단계: 상태 확인

```bash
# 전체 상태 확인
./scripts/status.sh

# 특정 서비스 상태 확인
./scripts/status.sh pods
```

### 5단계: 접속 확인

배포가 완료되면 다음 주소로 접속할 수 있습니다:

- **Frontend**: http://localhost:30001
- **API Gateway**: http://localhost:8080
- **API Health**: http://localhost:8080/actuator/health

## 🔧 상세 실행 가이드

### 완전한 배포 프로세스

```bash
# 1. 네임스페이스 생성
kubectl apply -f k8s/01-namespace.yaml

# 2. 설정 정보 적용
kubectl apply -f k8s/02-configmaps/
kubectl apply -f k8s/03-secrets/

# 3. 데이터베이스 배포
kubectl apply -f k8s/04-storage/

# 4. 애플리케이션 배포
kubectl apply -f k8s/05-applications/

# 5. 네트워크 설정
kubectl apply -f k8s/06-networking/
```

### 로컬 도메인 설정

Windows에서 로컬 도메인 접속을 위해 hosts 파일에 추가:

```powershell
# 관리자 권한으로 PowerShell 실행
Add-Content -Path "$env:windir\System32\drivers\etc\hosts" -Value "127.0.0.1 web3-community.local"
```

## 🔍 상태 확인 명령어

### 기본 상태 확인

```bash
# 전체 상태 보기
./scripts/status.sh

# Pod 상세 정보
./scripts/status.sh pods

# 문제 진단
./scripts/status.sh diagnose

# 리소스 사용량
./scripts/status.sh resources
```

### 로그 확인

```bash
# 전체 로그
./scripts/logs.sh all

# 특정 서비스 로그
./scripts/logs.sh show api-gateway

# 실시간 로그
./scripts/logs.sh follow mysql

# 로그 검색
./scripts/logs.sh search "error" user-service

# 여러 서비스 동시 보기
./scripts/logs.sh multi false 50 api-gateway user-service
```

### 서비스 관리

```bash
# 전체 재시작
./scripts/deploy.sh restart-all

# 특정 서비스 재시작
./scripts/deploy.sh restart api-gateway

# 전체 삭제
./scripts/deploy.sh delete
```

## 🏥 문제 해결

### 흔한 문제들

#### 1. Pod가 Pending 상태

```bash
# 상태 확인
kubectl describe pod -n web3-community <pod-name>

# 리소스 부족 확인
kubectl top nodes

# PVC 상태 확인
kubectl get pvc -n web3-community
```

#### 2. 이미지 풀 오류

```bash
# 로컬 이미지 빌드
docker build -t web3-community/api-gateway:latest ./docker/backend/api-gateway/

# 이미지 확인
docker images | grep web3-community
```

#### 3. 네트워크 연결 문제

```bash
# 서비스 연결 테스트
kubectl run test-pod --image=busybox --rm -it --restart=Never -- nslookup api-gateway-service.web3-community

# Ingress 상태 확인
kubectl get ingress -n web3-community
kubectl describe ingress web3-community-ingress -n web3-community
```

#### 4. 데이터베이스 연결 문제

```bash
# MySQL 연결 테스트
kubectl exec -it deployment/mysql-deployment -n web3-community -- mysql -u root -p

# MongoDB 연결 테스트
kubectl exec -it deployment/mongodb-deployment -n web3-community -- mongosh

# Redis 연결 테스트
kubectl exec -it deployment/redis-deployment -n web3-community -- redis-cli -a $REDIS_PASSWORD
```

## 🔄 개발 워크플로우

### 코드 변경 후 재배포

```bash
# 1. 이미지 재빌드
docker build -t web3-community/user-service:latest ./docker/backend/user-service/

# 2. 서비스 재시작
kubectl rollout restart deployment/user-service-deployment -n web3-community

# 3. 상태 확인
kubectl rollout status deployment/user-service-deployment -n web3-community
```

### 디버깅 모드

```bash
# Pod 내부 접속
kubectl exec -it deployment/user-service-deployment -n web3-community -- /bin/bash

# 포트 포워딩
kubectl port-forward service/api-gateway-service 8080:8080 -n web3-community

# 로그 실시간 확인
kubectl logs -f deployment/user-service-deployment -n web3-community
```

## 📊 모니터링

### 기본 모니터링

```bash
# Pod 상태 모니터링
watch -n 2 kubectl get pods -n web3-community

# 실시간 리소스 사용량
watch -n 5 ./scripts/status.sh resources

# Ingress 접속 테스트
watch -n 10 curl -I http://web3-community.local
```

### 고급 모니터링 (선택사항)

```bash
# Metrics Server 활성화 (Minikube)
minikube addons enable metrics-server

# Prometheus & Grafana 설치 (선택사항)
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm install prometheus prometheus-community/kube-prometheus-stack
```

## 🛠️ 고급 설정

### 성능 튜닝

```yaml
# k8s/05-applications/api-gateway/deployment.yaml 수정
resources:
  requests:
    memory: "512Mi"    # 메모리 증량
    cpu: "500m"        # CPU 증량
  limits:
    memory: "1Gi"
    cpu: "1000m"
```

### 개발 환경 최적화

```yaml
# Hot reload 설정
env:
- name: SPRING_DEVTOOLS_RESTART_ENABLED
  value: "true"
- name: JAVA_OPTS
  value: "-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005"
```

## 📚 추가 리소스

### 유용한 kubectl 명령어

```bash
# 전체 리소스 확인
kubectl get all -n web3-community

# 이벤트 확인
kubectl get events -n web3-community --sort-by='.lastTimestamp'

# 상세 설명
kubectl describe deployment api-gateway-deployment -n web3-community

# 설정 확인
kubectl get configmaps,secrets -n web3-community

# 로그 전체 보기
kubectl logs -f --all-containers=true -l app=api-gateway -n web3-community
```

### 자주 쓰는 스크립트

```bash
# 전체 재시작
./scripts/deploy.sh restart-all

# 전체 상태 확인
./scripts/status.sh all

# 에러 로그만 보기
./scripts/logs.sh search "error\|exception\|failed"

# 데이터베이스 상태 확인
./scripts/status.sh pods | grep -E "(mysql|mongodb|redis|kafka)"
```

## 🎯 다음 단계

1. **기본 기능 테스트**: 게시글 작성, 댓글, 사용자 관리
2. **인증 시스템**: JWT 토큰, OAuth2 연동
3. **실시간 기능**: WebSocket, 알림 시스템
4. **모니터링**: 로그, 메트릭, 알림
5. **CI/CD**: GitHub Actions 자동 배포

문제가 발생하면 [🔧 troubleshooting.md](troubleshooting.md)를 참고하세요!