# Web3 Community Backend - 아키텍처 설계 문서

## 목차
1. [프로젝트 개요](#1-프로젝트-개요)
2. [마이크로서비스 아키텍처 선택 이유](#2-마이크로서비스-아키텍처-선택-이유)
3. [전체 시스템 구조](#3-전체-시스템-구조)
4. [서비스별 설계](#4-서비스별-설계)
5. [기술 스택 선정 이유](#5-기술-스택-선정-이유)
6. [인증/인가 흐름](#6-인증인가-흐름)
7. [데이터 설계](#7-데이터-설계)
8. [장애 대응 전략](#8-장애-대응-전략)

---

## 1. 프로젝트 개요

Web3 커뮤니티 플랫폼의 백엔드 API 서버로, 사용자 인증·게시글·댓글 기능을 제공합니다.

| 항목 | 내용 |
|------|------|
| 언어 | Kotlin 1.9.22 |
| 프레임워크 | Spring Boot 3.2.2 |
| JDK | 17 |
| 빌드 도구 | Gradle (Kotlin DSL), 멀티모듈 구성 |

---

## 2. 마이크로서비스 아키텍처 선택 이유

### 왜 모놀리식이 아닌 마이크로서비스인가?

**독립적 확장성**
- 게시글 조회 트래픽이 높을 때는 `post-service`만 수평 확장
- 댓글의 SSE(Server-Sent Events) 스트리밍 부하를 `comment-service`에 격리
- 인증 서비스는 별도로 스케일 조정 가능

**기술 이질성 허용**
- `comment-service`만 WebFlux(Reactive)를 사용해 SSE 스트리밍을 구현하고,
  나머지 서비스는 익숙한 Spring MVC 사용
- 서비스별로 최적의 DB를 선택: MySQL(관계형 데이터) vs MongoDB(유연한 스키마)

**장애 격리**
- 댓글 서비스 장애가 게시글 조회에 영향을 미치지 않음
- Circuit Breaker로 특정 서비스 장애 전파 차단

**독립 배포**
- 각 서비스를 독립적으로 배포·롤백 가능
- 팀별로 서비스를 독립 개발 가능

---

## 3. 전체 시스템 구조

```
클라이언트 (Browser/Mobile)
        │
        ▼
┌─────────────────────┐
│  API Gateway (8080) │  ← JWT 검증, Rate Limiting, Circuit Breaker, CORS
└────────┬────────────┘
         │ X-User-Id / X-User-Email / X-User-Role / X-User-Nickname 헤더 전파
         │
    ┌────┴─────────────────────────────────┐
    │                                      │
    ▼                                      ▼
┌─────────────┐  ┌─────────────┐  ┌──────────────┐  ┌─────────────────┐
│ auth-service│  │ user-service│  │ post-service │  │ comment-service │
│   (8081)    │  │   (8082)    │  │   (8084)     │  │    (8083)       │
│  MySQL+Redis│  │    MySQL    │  │   MongoDB    │  │ MongoDB+Kafka   │
└─────────────┘  └─────────────┘  └──────────────┘  └─────────────────┘
      │                │                                     │
      │ (Feign 호출)   │                                     ▼
      └───────────────►│                              ┌─────────────┐
                                                      │    Kafka    │
                                                      │(comment-    │
                                                      │  events)    │
                                                      └─────────────┘
```

### 모듈 구성

| 모듈 | 포트 | DB | 특징 |
|------|------|-----|------|
| `common` | - | - | JWT, 공통 DTO, 예외 정의 (공유 라이브러리) |
| `api-gateway-service` | 8080 | Redis | JWT 필터, Rate Limiting, Circuit Breaker |
| `auth-service` | 8081 | MySQL + Redis | 인증, OAuth2, 토큰 관리 |
| `user-service` | 8082 | MySQL | 사용자 프로필 CRUD |
| `post-service` | 8084 | MongoDB | 게시글, 카테고리 CRUD |
| `comment-service` | 8083 | MongoDB + Kafka | 댓글 CRUD, SSE 스트리밍 |

---

## 4. 서비스별 설계

### 4.1 Common Module

공통 관심사를 분리하여 코드 중복 방지.

```
common/
├── constants/AppConstants.kt   # 헤더 이름 상수 (X-User-Id 등)
├── dto/
│   ├── ApiResponse.kt         # 통일된 API 응답 형식
│   └── PageResponse.kt        # 페이지 응답
├── exception/
│   ├── BusinessException.kt   # 비즈니스 예외 (ErrorCode 포함)
│   └── ErrorCode.kt           # 에러 코드 정의 (HTTP 상태 + 코드 + 메시지)
└── jwt/
    ├── JwtProperties.kt       # JWT 설정 (@ConfigurationProperties)
    └── JwtTokenProvider.kt    # JWT 생성/검증 (Gateway + Auth Service 공유)
```

**설계 포인트**: `JwtTokenProvider`를 공유 모듈에 두어 Gateway와 Auth Service가 동일한 로직으로 토큰을 처리.

---

### 4.2 API Gateway Service

**역할**: 모든 클라이언트 요청의 단일 진입점(Single Entry Point).

```
api-gateway-service/
├── config/
│   ├── GatewayConfig.kt        # JwtTokenProvider Bean 등록
│   └── RateLimiterConfig.kt    # IP 기반 Rate Limiter Key Resolver
├── controller/FallbackController.kt  # Circuit Breaker 폴백 응답
└── filter/JwtAuthGatewayFilterFactory.kt  # JWT 검증 + 사용자 정보 헤더 추입
```

**라우팅 규칙**:
- `/auth/**` → auth-service (JWT 필터 미적용, 공개 엔드포인트)
- `/api/users/**` → user-service (JWT 필터 적용)
- `/api/posts/**` → post-service (JWT 필터 + Circuit Breaker 적용)
- `/api/comments/**` → comment-service (JWT 필터 적용)

**핵심 필터 동작**:
```
요청 수신
  → Authorization 헤더 확인
  → JWT 서명/만료 검증
  → 클레임 추출 (userId, email, role, nickname)
  → 다운스트림 헤더 주입 (X-User-Id, X-User-Email, X-User-Role, X-User-Nickname)
  → 라우팅
```

---

### 4.3 Auth Service

**역할**: 회원가입, 로그인, OAuth2, 토큰 관리.

```
auth-service/
├── client/
│   ├── UserClient.kt           # Feign 클라이언트 (user-service 호출)
│   └── fallback/UserFallback.kt  # Circuit Breaker 폴백
├── config/
│   ├── JwtConfig.kt            # JwtTokenProvider Bean 등록
│   ├── OAuthConfig.kt          # RestTemplate 설정 (5s 타임아웃)
│   ├── OAuthProperties.kt      # Google/Naver OAuth 설정
│   └── SecurityConfig.kt       # Spring Security (전체 허용, 비밀번호 인코더)
├── entity/AuthCredential.kt    # 인증 정보 (이메일, 해시된 비밀번호, 닉네임, Provider)
├── service/
│   ├── AuthService.kt          # 인증 핵심 로직
│   ├── RefreshTokenService.kt  # Redis 기반 Refresh Token 관리
│   └── oauth/
│       ├── GoogleOAuthClient.kt  # Google OAuth2 토큰 교환 + 사용자 정보 조회
│       └── NaverOAuthClient.kt   # Naver OAuth2 토큰 교환 + 사용자 정보 조회
```

**인증 흐름**:
1. 회원가입 시 user-service에 프로필 생성 (Feign 호출)
2. 사용자 닉네임을 `AuthCredential`에 저장 (JWT 발급 시 포함용)
3. JWT Access Token: `userId` + `email` + `role` + `nickname` 포함
4. Refresh Token: Redis에 `refresh_token:{userId}` 키로 저장
5. 로그아웃: Access Token을 Redis 블랙리스트(`blacklist:{token}`)에 등록 + Refresh Token 삭제

---

### 4.4 User Service

**역할**: 사용자 프로필 CRUD (Auth Service에서 Feign으로 호출됨).

```
user-service/
├── controller/UserController.kt  # REST API
├── entity/User.kt               # 사용자 엔티티
├── repository/UserRepository.kt # JPA Repository
└── service/UserService.kt       # 비즈니스 로직
```

**Soft Delete**: `user.active = false`로 처리 (DB에서 실제 삭제 안 함).

---

### 4.5 Post Service

**역할**: 게시글과 카테고리 CRUD.

```
post-service/
├── controller/
│   ├── PostController.kt       # 게시글 API
│   └── CategoryController.kt  # 카테고리 API
├── document/
│   ├── Post.kt                # MongoDB Document
│   └── Category.kt            # MongoDB Document
└── service/
    ├── PostService.kt         # 게시글 비즈니스 로직
    └── CategoryService.kt     # 카테고리 비즈니스 로직
```

**Soft Delete**: `post.published = false`로 처리. `findByPublishedTrue` 쿼리로 게시된 글만 조회.

**기능**:
- 카테고리별, 태그별, 키워드 검색
- 조회수(`viewCount`) 증가
- 좋아요 토글 (`likedUserIds` Set으로 중복 방지)

---

### 4.6 Comment Service

**역할**: 댓글 CRUD + SSE 실시간 스트리밍 + Kafka 이벤트 발행.

```
comment-service/
├── controller/CommentController.kt  # REST + SSE 엔드포인트
├── document/Comment.kt             # MongoDB Document
├── service/
│   ├── CommentService.kt          # Reactive 비즈니스 로직 + SSE Sink 관리
│   └── CommentEventService.kt     # Kafka 이벤트 발행
└── dto/CommentEvent.kt            # Kafka 메시지 형식
```

**Reactive 선택 이유**: SSE를 위해 WebFlux 채택. `Sinks.Many<CommentResponse>`로 실시간 댓글 스트리밍.

**Soft Delete**: `comment.deleted = true` + 내용을 "This comment has been deleted."로 대체. 응답에서는 삭제된 댓글도 구조 유지 (트리 구조 보존).

**댓글 트리 구조**: `parentId`로 대댓글 구현. 조회 시 루트 댓글과 자식을 in-memory에서 조립.

---

## 5. 기술 스택 선정 이유

### 5.1 Kotlin

**선택 이유**:
- Java 대비 Null Safety로 NPE를 컴파일 타임에 방지
- Data class로 DTO를 간결하게 선언 (equals/hashCode/toString 자동 생성)
- 코루틴 및 Reactor와의 자연스러운 통합
- Spring Framework의 공식 지원 언어

### 5.2 Spring Boot 3.2 + Spring Cloud

**선택 이유**:
- Spring Cloud Gateway: 비동기/논블로킹 게이트웨이로 높은 처리량
- Spring Cloud OpenFeign: 선언적 HTTP 클라이언트로 서비스 간 통신 간소화
- Spring Cloud CircuitBreaker (Resilience4j): 장애 격리 표준화
- Spring Security: 유연한 인증/인가 체계

### 5.3 MySQL (Auth Service, User Service)

**선택 이유**:
- **Auth Service**: 인증 정보(이메일, 비밀번호, Provider)는 엄격한 무결성 필요 → RDBMS 적합
- **User Service**: 사용자 프로필은 정형화된 스키마, 복잡한 쿼리 없음
- JPA + Hibernate로 ORM 지원
- 트랜잭션 보장이 중요한 회원가입 로직

### 5.4 MongoDB (Post Service, Comment Service)

**선택 이유**:
- **Post Service**: 게시글은 태그(배열), 좋아요 목록(Set) 등 유연한 스키마 필요
- **Comment Service**: 댓글 이벤트, 동적 구조에 적합
- 자유로운 스키마 진화 (새 필드 추가 시 마이그레이션 불필요)
- `Spring Data MongoDB Reactive`로 WebFlux와 통합

### 5.5 Redis

**두 가지 용도로 활용**:

1. **Auth Service**: 토큰 관리
   - Refresh Token 저장 (TTL 자동 만료)
   - Access Token 블랙리스트 (로그아웃된 토큰 차단)
   - 메모리 DB의 빠른 읽기/쓰기로 토큰 검증 최적화

2. **API Gateway**: Rate Limiting
   - Redis 기반 `RequestRateLimiter` (Token Bucket 알고리즘)
   - IP 기반 요청 제한: 초당 10회, 최대 버스트 20회
   - 분산 환경에서 인스턴스 간 공유 카운터 유지

### 5.6 Apache Kafka (Comment Service)

**선택 이유**:
- 댓글 생성 이벤트를 비동기로 발행 (게시글 서비스의 댓글 수 업데이트 등)
- 서비스 간 느슨한 결합(Loose Coupling)
- 이벤트 소싱 패턴 지원 가능
- 높은 처리량과 내구성

**토픽 구조**:
- `comment-events`: 댓글 생성/수정/삭제 이벤트
- 파티션 키: `postId` (같은 게시글의 댓글 이벤트는 순서 보장)

### 5.7 WebFlux (Comment Service만 Reactive)

**선택 이유**:
- SSE(Server-Sent Events) 구현: 클라이언트에 실시간 댓글 푸시
- 논블로킹 I/O로 수천 개의 SSE 연결을 적은 스레드로 처리
- `Sinks.Many`로 멀티캐스트 스트리밍

**나머지 서비스가 MVC를 사용하는 이유**:
- 대부분의 CRUD는 동기 방식이 더 직관적이고 디버깅 용이
- Reactive 스타일의 학습 곡선을 필요한 서비스에만 적용
- 팀 생산성 고려

### 5.8 Resilience4j (Circuit Breaker)

**선택 이유**:
- `post-service`는 Gateway에서 Circuit Breaker 적용 (슬라이딩 윈도우: 10회, 실패율 50% 초과 시 Open)
- `auth-service`는 user-service Feign 호출에 Circuit Breaker 적용
- Open 상태 시 폴백 응답으로 사용자 경험 유지
- Resilience4j는 Spring Boot 3.x 공식 지원, Hystrix 대체

### 5.9 JWT (JSON Web Token)

**선택 이유**:
- **Stateless 인증**: 서버 세션 없이 토큰 자체에 사용자 정보 포함
- 마이크로서비스 환경에서 각 서비스가 독립적으로 토큰 검증 가능
- Gateway에서 1회 검증 후 다운스트림에 헤더로 전파 (각 서비스는 헤더 신뢰)
- Access Token(1h) + Refresh Token(7d) 이중 토큰 전략

**클레임 구조**:
```json
{
  "sub": "1",           // userId (subject)
  "userId": 1,
  "email": "user@example.com",
  "role": "USER",
  "nickname": "홍길동",
  "iss": "web3-community",
  "iat": 1234567890,
  "exp": 1234571490
}
```

### 5.10 Spring Cloud OpenFeign

**선택 이유**:
- auth-service → user-service 호출 시 선언적 HTTP 클라이언트
- RestTemplate/WebClient 직접 사용보다 코드 간결
- Circuit Breaker와 자동 통합 (`spring.cloud.openfeign.circuitbreaker.enabled: true`)
- 폴백(Fallback) 클래스로 Circuit Open 시 대체 로직 정의

### 5.11 springdoc-openapi 2.3.0

**선택 이유**:
- Spring Boot 3.x 지원 (springfox는 지원 안 함)
- MVC 서비스: `springdoc-openapi-starter-webmvc-ui`
- WebFlux 서비스(comment): `springdoc-openapi-starter-webflux-ui`
- Swagger UI를 통한 API 문서 자동 생성

---

## 6. 인증/인가 흐름

### 6.1 일반 로그인

```
클라이언트
  → POST /auth/login {email, password}
  → [Gateway: 필터 미적용]
  → Auth Service
    → DB에서 AuthCredential 조회
    → BCrypt 비밀번호 검증
    → JWT Access Token 생성 (userId, email, role, nickname 포함)
    → Refresh Token 생성 → Redis 저장 (TTL: 7일)
  ← 응답: {accessToken, refreshToken, ...}
```

### 6.2 소셜 로그인 (OAuth2)

```
클라이언트
  → 소셜 제공자에서 Authorization Code 획득
  → POST /auth/oauth/login {provider, code, redirectUri}
  → Auth Service
    → provider OAuth API 호출: code → access_token
    → provider 사용자 정보 조회 (email, nickname, providerId)
    → DB에서 provider+providerId로 기존 계정 검색
    → [신규] user-service에 프로필 생성 (Feign) + AuthCredential 저장
    → [기존] 기존 AuthCredential 사용
    → JWT 발급
  ← 응답: {accessToken, refreshToken, ...}
```

### 6.3 인증이 필요한 API 호출

```
클라이언트
  → GET /api/posts (Authorization: Bearer {accessToken})
  → [Gateway: JwtAuthGatewayFilterFactory]
    → Bearer 토큰 추출
    → jwtTokenProvider.validateToken() 검증 (서명 + 만료)
    → 클레임 추출 → X-User-Id, X-User-Email, X-User-Role, X-User-Nickname 헤더 추가
  → Post Service
    → 헤더에서 사용자 정보 읽어 비즈니스 로직 수행
  ← 응답 반환
```

### 6.4 로그아웃 + 토큰 블랙리스트

```
클라이언트
  → POST /auth/logout (Authorization: Bearer {accessToken})
  → Auth Service
    → Redis: blacklist:{token} = "true" (TTL: 남은 만료 시간)
    → Redis: refresh_token:{userId} 삭제
  ← 응답 반환

※ 로그아웃 후 Access Token으로 요청 시:
  → Gateway: JWT 서명/만료는 유효 (토큰 자체는 멀쩡)
  → Auth Service /auth/validate: 블랙리스트 체크로 거부 (UNAUTHORIZED)
```

---

## 7. 데이터 설계

### 7.1 MySQL - auth_credentials

```sql
CREATE TABLE auth_credentials (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    email       VARCHAR(100) UNIQUE NOT NULL,
    nickname    VARCHAR(50) NOT NULL,
    password    VARCHAR(255),           -- NULL: 소셜 로그인 계정
    role        VARCHAR(20) NOT NULL,   -- USER | ADMIN | MODERATOR
    user_id     BIGINT NOT NULL,        -- user-service의 사용자 ID
    enabled     BOOLEAN NOT NULL,
    provider    VARCHAR(20) NOT NULL,   -- LOCAL | GOOGLE | NAVER
    provider_id VARCHAR(255),           -- 소셜 제공자의 고유 ID
    created_at  DATETIME,
    updated_at  DATETIME
);
```

### 7.2 MySQL - users

```sql
CREATE TABLE users (
    id                BIGINT PRIMARY KEY AUTO_INCREMENT,
    email             VARCHAR(100) UNIQUE NOT NULL,
    nickname          VARCHAR(50) UNIQUE NOT NULL,
    bio               VARCHAR(500),
    profile_image_url VARCHAR(255),
    role              VARCHAR(20) NOT NULL DEFAULT 'USER',
    active            BOOLEAN NOT NULL DEFAULT TRUE,
    created_at        DATETIME,
    updated_at        DATETIME
);
```

### 7.3 MongoDB - posts

```json
{
  "_id": "ObjectId",
  "title": "게시글 제목",
  "content": "내용",
  "authorId": 1,
  "authorNickname": "홍길동",
  "categoryId": "ObjectId",
  "categoryName": "자유게시판",
  "tags": ["web3", "blockchain"],
  "viewCount": 100,
  "likeCount": 10,
  "commentCount": 5,
  "likedUserIds": [1, 2, 3],
  "published": true,
  "createdAt": "2026-01-01T00:00:00",
  "updatedAt": "2026-01-01T00:00:00"
}
```

> **Soft Delete**: `published: false`로 처리. 삭제된 게시글은 조회 쿼리에서 제외.

### 7.4 MongoDB - comments

```json
{
  "_id": "ObjectId",
  "postId": "게시글 ID",
  "parentId": null,
  "depth": 0,
  "authorId": 1,
  "authorNickname": "홍길동",
  "content": "댓글 내용",
  "likeCount": 3,
  "likedUserIds": [1, 2],
  "deleted": false,
  "createdAt": "2026-01-01T00:00:00",
  "updatedAt": "2026-01-01T00:00:00"
}
```

> **Soft Delete**: `deleted: true`로 처리. 응답 시 작성자는 "Deleted", 내용은 "This comment has been deleted."로 표시.

### 7.5 Redis 키 구조

```
refresh_token:{userId}     → Refresh Token 문자열 (TTL: 7일)
blacklist:{accessToken}    → "true" (TTL: 토큰 남은 만료 시간)

rate_limit:request_count   → Redis RequestRateLimiter 내부 키
```

---

## 8. 장애 대응 전략

### 8.1 Circuit Breaker (Resilience4j)

| 위치 | 대상 | 설정 |
|------|------|------|
| Gateway | post-service | 슬라이딩 윈도우 10회, 실패율 50% 초과 시 Open |
| auth-service | user-service (Feign) | OpenFeign + CB 통합 |

**Open 상태 시 폴백**:
- post-service: `GET /fallback/post` → `{"success": false, "errorCode": "SERVICE_UNAVAILABLE"}`
- user-service (auth): `UserFallback` → `BusinessException(SYSTEM_BUSY)` 발생 → 503 응답

### 8.2 Rate Limiting

- 알고리즘: Token Bucket
- 기본 설정 (local): 초당 10 토큰, 버스트 최대 20
- 키: 클라이언트 IP 주소 기반
- 초과 시: HTTP 429 Too Many Requests

### 8.3 SSE 연결 관리

- `commentSinks: ConcurrentHashMap<postId, Sinks.Many>`
- 구독자가 연결 해제 시 (`doFinally`) 구독자 수 확인 후 Sink 제거
- ConcurrentHashMap으로 멀티스레드 안전

### 8.4 OAuth 타임아웃

- Google/Naver OAuth API 호출: 연결 5초, 읽기 5초 타임아웃
- 타임아웃/오류 시 `OAUTH_AUTHENTICATION_FAILED` (401) 반환

### 8.5 환경별 프로파일

| 프로파일 | DDL | 로깅 | 비고 |
|---------|-----|------|------|
| `local` | update | DEBUG | 로컬 개발 (기본) |
| `dev` | validate | INFO | 개발 서버, 환경변수 기반 |
| `prod` | validate | WARN | 운영 서버, 환경변수 필수 |
