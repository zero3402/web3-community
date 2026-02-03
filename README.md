# 🌐 Web3 Community Platform(현재 만드는 중입니다.)

## 📋 프로젝트 개요

MSA(Microservices Architecture) 기반의 Web3 커뮤니티 플랫폼입니다. 쿠버네티스 환경에서 실행되도록 설계되었습니다.

### 🏗️ 아키텍처 구성

**Frontend:**
- Vue.js 3 (TypeScript 지원)
- 반응형 디자인
- 실시간 알림 기능

**Backend (마이크로서비스):**
- **API Gateway**: Spring Cloud Gateway (라우팅, 인증)
- **User Service**: 사용자 관리 (MySQL)
- **Post Service**: 게시글 관리 (MongoDB)
- **Comment Service**: 댓글 관리 (MongoDB)
- **Auth Service**: 인증/인가 (Redis 세션)
- **Notification Service**: 알림 처리 (Kafka)

**데이터베이스:**
- MySQL: 사용자 정보, 시스템 설정
- MongoDB: 게시글, 댓글 (문서 기반)
- Redis: 세션, 캐시, 실시간 데이터
- Kafka: 이벤트 기반 메시징

## ⚡ 5분 만에 시작하기

### 📋 사전 준비물

**Windows 환경:**
- Windows 10/11 (64bit)
- Docker Desktop (설치 필요)
- Git for Windows
- 최소 8GB RAM, 20GB 여유 디스크

### 🚀 빠른 시작

```bash
# 1. 프로젝트 클론
git clone <repository-url>
cd web3-community

# 2. 환경 설정 (초기 실행 한 번만)
./scripts/setup.sh

# 3. 전체 배포
./scripts/deploy.sh

# 4. 상태 확인
./scripts/status.sh

# 5. 접속 확인
# 브라우저에서 http://localhost:30001 접속
```

## 🔍 기본 명령어

### 📊 상태 확인
```bash
# 전체 상태 확인
./scripts/status.sh

# 파드 상태 확인
kubectl get pods -n web3-community

# 서비스 목록 확인
kubectl get svc -n web3-community
```

### 📝 로그 확인
```bash
# 전체 로그
./scripts/logs.sh

# 특정 서비스 로그
./scripts/logs.sh api-gateway
./scripts/logs.sh user-service
```

### 🔄 서비스 관리
```bash
# 특정 서비스 재시작
./scripts/restart.sh api-gateway

# 전체 재시작
./scripts/restart.sh

# 전체 삭제
./scripts/delete.sh
```

## 🏗️ 프로젝트 구조

```
web3-community/
├── 📋 README.md                    # 프로젝트 설명서
├── 🔧 .env                         # 환경 변수 설정
├── 🐳 docker/                      # Docker 파일들
│   ├── frontend/Dockerfile
│   └── backend/
│       ├── api-gateway/Dockerfile
│       ├── user-service/Dockerfile
│       ├── post-service/Dockerfile
│       ├── comment-service/Dockerfile
│       ├── auth-service/Dockerfile
│       └── notification-service/Dockerfile
├── ☸️ k8s/                        # 쿠버네티스 매니페스트
│   ├── 🌐 01-namespace.yaml
│   ├── ⚙️ 02-configmaps/
│   ├── 🔒 03-secrets/
│   ├── 💾 04-storage/
│   ├── 🚀 05-applications/
│   └── 🌍 06-networking/
├── 🛠️ scripts/                    # 자동화 스크립트
│   ├── setup.sh                  # 초기 환경 설정
│   ├── deploy.sh                 # 전체 배포
│   ├── delete.sh                 # 전체 삭제
│   ├── restart.sh                # 서비스 재시작
│   ├── logs.sh                   # 로그 확인
│   ├── status.sh                 # 상태 확인
│   └── debug.sh                  # 디버깅 도구
├── 📚 docs/                       # 문서
│   ├── getting-started.md        # 상세 시작 가이드
│   ├── architecture.md            # 아키텍처 설명
│   ├── troubleshooting.md        # 문제 해결
│   └── k8s-basics.md             # 쿠버네티스 기초
├── 🎨 frontend/                   # Vue.js 프론트엔드
└── ⚙️ backend/                    # Spring Boot 백엔드
    ├── api-gateway/
    ├── user-service/
    ├── post-service/
    ├── comment-service/
    ├── auth-service/
    └── notification-service/
```

## 🌟 주요 기능

### 🔐 인증/인가 시스템
- JWT 토큰 기반 인증
- Redis 세션 관리
- 권한 기반 접근 제어 (RBAC)

### 📝 게시판 기능
- 실시간 게시글 작성/수정/삭제
- 계층적 댓글 시스템
- 검색 및 필터링

### 🔔 알림 시스템
- Kafka 기반 실시간 알림
- 웹소켓 연동
- 알림 설정 관리

### 👥 사용자 관리
- 회원가입/로그인/프로필 관리
- 소셜 로그인 (OAuth2)
- 사용자 권한 관리

## 🔧 개발 환경 설정

### 🐳 Docker 이미지 빌드
```bash
# 프론트엔드 빌드
docker build -t web3-community/frontend:latest ./docker/frontend/

# 백엔드 서비스들 빌드
docker build -t web3-community/api-gateway:latest ./docker/backend/api-gateway/
docker build -t web3-community/user-service:latest ./docker/backend/user-service/
# ... 기타 서비스들
```

### 🔄 개발 사이클
```bash
# 코드 변경 후 자동 재배포
docker build -t web3-community/user-service:dev ./docker/backend/user-service/
kubectl rollout restart deployment/user-service -n web3-community
```

## 📊 모니터링

### 🔍 기본 모니터링
```bash
# 리소스 사용량 확인
kubectl top pods -n web3-community

# 파드 상세 정보
kubectl describe pod <pod-name> -n web3-community

# 서비스 엔드포인트 확인
kubectl get endpoints -n web3-community
```

### 📈 상세 모니터링 (선택사항)
- Prometheus + Grafana (메트릭 수집)
- ELK Stack (로그 분석)
- Jaeger (분산 추적)

## 🐛 문제 해결

### 🚨 흔한 문제들
1. **Pod가 계속 Pending 상태**: `kubectl describe pod`로 리소스 부족 확인
2. **Connection refused**: 서비스 이름 및 네임스페이스 확인
3. **Image pull error**: Docker 이미지 로컬 빌드 확인
4. **Volume mount error**: PVC 상태 확인

### 🔧 디버깅 도구
```bash
# 디버그 스크립트 실행
./scripts/debug.sh

# 포트 포워딩으로 로컬 접속
kubectl port-forward svc/api-gateway 8080:8080 -n web3-community

# 파드 내부 접속
kubectl exec -it <pod-name> -n web3-community -- /bin/bash
```

## 📖 학습 자료

- [📚 getting-started.md](./docs/getting-started.md) - 상세 시작 가이드
- [🏗️ architecture.md](./docs/architecture.md) - 아키텍처 설명
- [🔧 troubleshooting.md](./docs/troubleshooting.md) - 문제 해결
- [☸️ k8s-basics.md](./docs/k8s-basics.md) - 쿠버네티스 기초

## 🤝 기여하기

1. Fork this repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📄 라이선스

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🎯 다음 단계

1. **기본 기능 구현**: 게시판 CRUD, 댓글 기능
2. **인증 시스템**: JWT, OAuth2 구현
3. **실시간 기능**: 웹소켓, 알림 시스템
4. **모니터링**: 로그, 메트릭, 분산 추적
5. **CI/CD**: GitHub Actions 자동 배포

---

**Happy Coding! 🚀**

처음이시면 [📚 시작 가이드](./docs/getting-started.md)를 먼저 읽어보세요!
