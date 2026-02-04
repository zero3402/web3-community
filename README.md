# Web3 Community - Docker Compose

## 🏗️ 전체 아키텍처

이 Docker Compose는 Web3 Community 플랫폼의 완전한 인프라를 위한 모든 컴포넌트를 포함합니다.

### 📦 **Infrastructure Services**
- **MySQL 8.0** - 관계형 데이터베이스
- **Redis 7-alpine** - 캐싱 및 세션 저장
- **Nginx Alpine** - 리버스 프록시 및 로드 밸런싱

### 🌐 **Platform Services**
- **Eureka Server** - 서비스 디스커버리
- **API Gateway** - Spring Cloud Gateway + 루트롤
- **Nginx Load Balancer** - 외부 루드밸런싱

### 🧑 **DDD Microservices**
- **User Service** (Port 8081) - 사용자 관리
- **Auth Service** (Port 8082) - 인증/인가
- **Post Service** (Data Port 8083) - 컨텐츠 관리
- **Comment Service** (Port 8084) - 댓글 시스템
- **Notification Service** (Port 8085) - 알림 서비스
- **Analytics Service** (Data Port 8086) - 분석 서비스

### 🌐 **Frontend**
- **React SPA** (Port 3000) - 웹 프론트앤드

## 🚀 **사용 방법**

### 모든 서비스 시작:
```bash
./start-all.sh
```

### 모든 서비스 중지:
```bash
./stop-all.sh
```

### 특정 서비스만 시작:
```bash
# 인프라
docker-compose up -d mysql redis eureka

# 마이크로서비스
docker-compose up -d user-service auth-service

# 프론트엔드
docker-compose up -d frontend

# 부분 서비스 (커맨드)
docker-compose up -d user-service post-service comment-service
```

### 로그 확인:
```bash
# 특정 서비스 로그
docker-compose logs user-service
docker-compose logs -f auth-service

# 모든 서비스 로그
docker-compose logs

# 특정 서비스 실시간 로그
docker-compose logs -f post-service --tail=100
```

### 건강 상태 확인:
```bash
# 전체 상태
docker-compose ps

# 특정 서비스 건강 체크
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health
```

## 🔗 **네트워크 구성**
- **web3-network**: 모든 서비스가 통신하는 내부 네트워크
- **외부 노출 포트**: 80 (Nginx), 3000 (Frontend)
- **내부 포트**: MySQL(3306), Redis(6379), Eureka(8761)

## 📁 **데이터 지속성**
- **mysql_data**: MySQL 데이터 영구 보존
- **redis_data**: Redis 데이터 영구 보존
- **logs/**: 각 서비스 로그 저장
- **uploads/**: 파일 업로드 저장

## 🔧 **환경 변수**
- `EMAIL_USERNAME`: 이메일 발송용 사용자명
- `EMAIL_PASSWORD`: 이메일 발송용 비밀번호
- `FIREBASE_SERVER_KEY`: FCM 서버 키 (선택사항)

## 🔗 **서비스 통신 흐름**
```
[사용자] → [Nginx:80] → [Gateway:8080] → [User:8081]
                ↓                   ↓                    ↓
[사용자] → [Gateway:8080] → [Auth:8082]
[사용자] → [Gateway:8080] → [Post:8083] → [Analytics:8086]
[사용자] → [Gateway:8080] → [Comment:8084] → [Analytics:8086]
[알림] → [Gateway:8080] → [Notification:8085]
```

## 📊 **모니터링**
- 각 서비스 Actuator 엔드포인트 (/actuator/health)
- 중앙화된 로깅 (logs/ 디렉토리)
- 컨테이너 영구 건강 체크
- 메트릭스 수집 (Prometheus 지원)

## 🔒 **보안 고려사항**
1. **프로덕션 환경에서는 실제 비밀번호를 환경 변수로 설정
2. 데이터베이스는 영구 저장되지만, 개발 환경에서는 도커 데이터 손실 가능성 있음
3. 포트 매핑을 실제 환경에 맞게 조정 필요
4. HTTPS 설정이 필요한 경우 Nginx SSL 인증서 구성 필요
5. 데이터베이스 백업 전략 전략 정책 필요

---
**🎉 완전한 Web3 Community 플랫폼이 Docker 환경에서 실행 가능합니다!**