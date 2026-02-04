# Web3 Community - Clean Hybrid Architecture

불필요한 코드를 제거하고 핵심 기능에 집중한 간소화된 하이브리드 MVC/WebFlux 마이크로서비스

## 🏗️ 서비스 구조

### 📋 User Service - Spring MVC (동기식)
- **포트**: 8081
- **핵심 기능**: 사용자 등록, 로그인, 정보 조회
- **API**: 
  - `POST /api/auth/register` - 회원가입
  - `POST /api/auth/login` - 로그인
  - `GET /api/auth/users/{id}` - 사용자 정보

### 📝 Post Service - Spring WebFlux (리액티브)
- **포트**: 8082
- **핵심 기능**: 게시글 CRUD, 검색, 좋아요
- **API**:
  - `POST /api/posts` - 게시글 생성
  - `GET /api/posts` - 전체 게시글
  - `GET /api/posts/{id}` - 게시글 상세
  - `GET /api/posts/search?query=` - 검색
  - `POST /api/posts/{id}/like` - 좋아요

### 🔔 Notification Service - Spring WebFlux (리액티브)
- **포트**: 8083
- **핵심 기능**: 알림 생성, 조회, SSE 스트리밍
- **API**:
  - `POST /api/notifications` - 알림 생성
  - `GET /api/notifications/user/{id}` - 사용자 알림
  - `GET /api/notifications/user/{id}/unread` - 안읽은 알림
  - `GET /api/notifications/user/{id}/stream` - SSE 스트림

### 🌐 API Gateway
- **포트**: 8080
- **역할**: 모든 요청을 적절한 서비스로 라우팅

## 🚀 빠른 시작

```bash
# 전체 서비스 시작
./start-services.sh

# 개별 서비스 실행
cd services/user-service      # MVC
./gradlew bootRun

cd services/post-service       # WebFlux  
./gradlew bootRun

cd services/notification-service  # WebFlux
./gradlew bootRun
```

## 📋 핵심 API 사용 예제

### 사용자 관리 (MVC)
```bash
# 회원가입
curl -X POST http://localhost:8080/api/auth/register \
  -H 'Content-Type: application/json' \
  -d '{
    "username": "testuser",
    "email": "test@example.com", 
    "password": "password123",
    "displayName": "Test User"
  }'

# 로그인
curl -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{
    "email": "test@example.com",
    "password": "password123"
  }'

# 사용자 정보 조회
curl http://localhost:8080/api/auth/users/1
```

### 게시글 관리 (WebFlux)
```bash
# 게시글 생성
curl -X POST http://localhost:8080/api/posts \
  -H 'Content-Type: application/json' \
  -d '{
    "title": "My First Post",
    "content": "This is my post content",
    "authorId": 1,
    "authorName": "Test User",
    "category": "general",
    "tags": ["web3", "community"]
  }'

# 게시글 목록 (리액티브 스트림)
curl http://localhost:8080/api/posts

# 게시글 검색
curl http://localhost:8080/api/posts/search?query=web3

# 좋아요
curl -X POST http://localhost:8080/api/posts/post-id/like

# 태그로 검색
curl http://localhost:8080/api/posts/search/tag?tag=web3
```

### 알림 관리 (WebFlux)
```bash
# 알림 생성
curl -X POST http://localhost:8080/api/notifications \
  -H 'Content-Type: application/json' \
  -d '{
    "userId": 1,
    "title": "New Post",
    "message": "Someone posted in your community",
    "type": "POST_CREATED",
    "relatedId": "post-id"
  }'

# 사용자 알림 목록
curl http://localhost:8080/api/notifications/user/1

# 안읽은 알림
curl http://localhost:8080/api/notifications/user/1/unread

# 안읽은 알림 수
curl http://localhost:8080/api/notifications/user/1/unread/count

# 실시간 알림 스트림 (SSE)
curl -N -H "Accept: text/event-stream" \
  http://localhost:8080/api/notifications/user/1/stream

# 알림 읽음 처리
curl -X PUT http://localhost:8080/api/notifications/notification-id/read

# 모든 알림 읽음 처리
curl -X PUT http://localhost:8080/api/notifications/user/1/read-all
```

## 🛠️ 기술 스택

| 서비스 | 프레임워크 | 데이터베이스 | 특징 |
|--------|------------|------------|------|
| User Service | Spring MVC | MySQL | 동기식, 트랜잭션 |
| Post Service | Spring WebFlux | MongoDB | 리액티브, 높은 동시성 |
| Notification Service | Spring WebFlux | MongoDB + Kafka | 실시간 스트리밍 |
| API Gateway | Spring Cloud Gateway | - | 통합 라우팅 |

## ✨ 정리된 기능

### ✅ 유지된 핵심 기능
- 사용자 인증/인가
- 게시글 CRUD 및 검색
- 실시간 알림 및 SSE
- 간소화된 데이터 모델
- 효율적인 에러 처리

### ❌ 제거된 불필요 기능
- 복잡한 유틸리티 클래스
- 과도한 유효성 검사
- 불필요한 미들웨어
- 중복된 코드
- 복잡한 설정

## 📊 성능 특징

- **User Service**: 안정적인 MVC 처리, 트랜잭션 보장
- **Post Service**: 높은 동시성 처리, 메모리 효율성
- **Notification Service**: 실시간 스트리밍, SSE 지원
- **통합**: API Gateway를 통한 단일 진입점

이제 각 서비스는 자신의 역할에 최적화된 기술 스택으로 동작하며, 불필요한 코드가 제거되어 유지보수가 쉽습니다.