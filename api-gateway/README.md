# API Gateway

Web3 커뮤니티 MSA 아키텍처의 단일 진입점(Single Entry Point)으로,
모든 클라이언트 요청을 수신하여 적절한 내부 마이크로서비스로 라우팅한다.

---

## 목차

1. [기술 스택](#기술-스택)
2. [기술 선정 이유](#기술-선정-이유)
3. [아키텍처 다이어그램](#아키텍처-다이어그램)
4. [라우팅 규칙](#라우팅-규칙)
5. [필터 파이프라인](#필터-파이프라인)
6. [Rate Limiting](#rate-limiting)
7. [환경 변수](#환경-변수)
8. [실행 방법](#실행-방법)
9. [모니터링](#모니터링)

---

## 기술 스택

| 항목 | 기술 | 버전 |
|------|------|------|
| 프레임워크 | Spring Cloud Gateway | 4.x (Spring Boot 3.3) |
| 런타임 | Kotlin + Coroutines | 1.9 |
| 서버 엔진 | Netty (Webflux) | - |
| 캐시/Rate Limit | Redis (Reactive) | 7.x |
| 서비스 디스커버리 | Netflix Eureka Client | - |
| JVM | OpenJDK 21 (GraalVM 호환) | LTS |

---

## 기술 선정 이유

### Spring Cloud Gateway vs Nginx

| 비교 항목 | Spring Cloud Gateway | Nginx |
|-----------|---------------------|-------|
| 언어/생태계 | Java/Kotlin, Spring 생태계 통합 | C, Nginx 설정 언어 |
| 서비스 디스커버리 | Eureka, Consul 네이티브 연동 | 별도 플러그인 필요 |
| 동적 라우팅 | 런타임 변경 가능 (코드 기반) | 재시작 필요 |
| 커스텀 필터 | Java/Kotlin으로 자유롭게 구현 | Lua 스크립트 또는 모듈 개발 필요 |
| JWT 검증 | Spring Security 연동 자연스러움 | 외부 인증 서버 필요 |
| 성능 | Netty 비동기: 수만 동시 연결 처리 | C 기반으로 원시 성능은 우월 |
| 운영 편의성 | Spring 팀이라면 학습 비용 최소 | 인프라 팀 필요 |

**선택 이유**: 이 프로젝트는 Spring Boot + Kotlin 스택을 전면 사용하므로,
동일 언어로 JWT 검증, Rate Limiting, 커스텀 필터를 구현할 수 있는
Spring Cloud Gateway가 운영 일관성과 개발 생산성 면에서 최적이다.
단순 프록시가 아닌 비즈니스 로직(인증, 제한)이 Gateway에 있으므로
코드로 제어하는 것이 Nginx 설정보다 안전하고 테스트 가능하다.

### Webflux(Netty) vs MVC(Tomcat)

| 비교 항목 | Webflux / Netty | MVC / Tomcat |
|-----------|----------------|--------------|
| I/O 모델 | 비동기 논블로킹 | 동기 블로킹 |
| 스레드 모델 | 이벤트 루프 (코어 수 기반) | 요청당 1 스레드 |
| 동시 연결 | 수만 ~ 수십만 | 스레드 풀 크기에 비례 |
| 메모리 | 낮음 (스레드 스택 불필요) | 스레드당 ~1MB |
| 코드 복잡도 | 리액티브/Coroutines 이해 필요 | 직관적인 동기 코드 |

**선택 이유**: API Gateway는 자체 비즈니스 로직보다 I/O(요청 전달)가 대부분이다.
Netty의 비동기 논블로킹 모델은 수많은 동시 연결을 적은 스레드로 처리할 수 있어,
Gateway 역할에 최적화되어 있다. Kotlin Coroutines를 사용하면 복잡한 리액티브
코드를 동기 코드처럼 읽기 쉽게 작성할 수 있다.

### Redis Sliding Window Rate Limiting

| 알고리즘 | 장점 | 단점 |
|---------|------|------|
| Fixed Window | 구현 단순 | 경계에서 2배 요청 허용 가능 |
| Sliding Window | 정확한 rate 제어 | 구현 복잡, 메모리 사용 높음 |
| Token Bucket | 버스트 허용, 유연 | 구현 복잡 |
| Leaky Bucket | 일정한 처리율 | 순간 트래픽 처리 불가 |

**선택 이유**: 블록체인 전송 등 비용이 발생하는 API를 보호해야 하므로
경계 문제가 없는 Sliding Window를 채택했다. Redis ZSet(Sorted Set)의
타임스탬프 기반 정렬을 활용하여 O(log N) 시간 복잡도로 구현한다.

---

## 아키텍처 다이어그램

```
                         ┌─────────────────────────────────────────┐
                         │              API Gateway                 │
Vue.js (5173)  ─────────►│                                         │
                         │  LoggingFilter → RateLimitFilter         │
Mobile App     ─────────►│       → JwtAuthFilter → Route           │
                         │                                         │
                         └───┬───────────┬───────┬────────┬────────┘
                             │           │       │        │
                        lb://auth   lb://user  lb://board lb://blockchain
                             │           │       │        │
                         ┌───▼──┐  ┌───▼──┐ ┌──▼──┐ ┌──▼──────┐
                         │ Auth │  │ User │ │Board│ │Blockchain│
                         │ :8081│  │ :8082│ │:8083│ │  :8084  │
                         └───┬──┘  └───┬──┘ └──┬──┘ └──┬──────┘
                             │         │        │        │
                         ┌───▼─────────▼────────▼────────▼────────┐
                         │              Eureka Server               │
                         │              (Service Registry)          │
                         └─────────────────────────────────────────┘
```

---

## 라우팅 규칙

| 클라이언트 경로 | 대상 서비스 | JWT 인증 | 비고 |
|---------------|------------|---------|------|
| `/api/v1/auth/**` | auth-service | 불필요 | 로그인, 회원가입, 토큰 갱신 |
| `/api/v1/users/**` | user-service | 필요 | 회원 프로필, 지갑 정보 |
| `/api/v1/boards/**` | board-service | 필요 | 게시글 CRUD, 좋아요 |
| `/api/v1/blockchain/**` | blockchain-service | 필요 | 지갑 생성/전송 |

### StripPrefix 동작

```
클라이언트 요청:   GET /api/v1/boards/123
                       ↓ StripPrefix=2 (/api/v1 제거)
board-service 수신: GET /boards/123
```

### X-User-Id 헤더 전달

```
클라이언트: Authorization: Bearer eyJhbGci...
               ↓ JwtAuthFilter: 토큰 검증 후 userId 추출
downstream:  X-User-Id: user-uuid-here
             X-User-Role: USER
```

---

## 필터 파이프라인

요청이 들어오면 아래 순서로 필터가 실행된다.

```
요청 수신
    │
    ▼
┌─────────────────┐
│  LoggingFilter  │  X-Request-ID 주입, 요청 로그 기록
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ RateLimitFilter │  Redis Sliding Window로 Rate Limit 검사
└────────┬────────┘  초과 시 → 429 반환
         │
         ▼
┌─────────────────┐
│  JwtAuthFilter  │  Authorization 헤더 검증
└────────┬────────┘  실패 시 → 401/403 반환
         │           성공 시 → X-User-Id 헤더 추가
         ▼
┌─────────────────┐
│  Route 매칭     │  lb://서비스명으로 Eureka 조회 + 로드밸런싱
└────────┬────────┘
         │
         ▼
    downstream 서비스
         │
         ▼
┌─────────────────┐
│  LoggingFilter  │  응답 상태 코드, 처리 시간 로그 기록
└─────────────────┘
```

---

## Rate Limiting

| 기준 | 분당 허용 | 적용 대상 |
|------|---------|---------|
| IP 기준 | 100 req/min | 미인증 요청 (`/api/v1/auth/**`) |
| 사용자 기준 | 300 req/min | 인증된 모든 요청 |
| 블록체인 전송 | 30 req/min | `/api/v1/blockchain/**` (비용 발생 API 보호) |

### 응답 헤더

```
HTTP/1.1 200 OK
X-RateLimit-Limit: 300
X-RateLimit-Remaining: 297
X-RateLimit-Reset: 58

HTTP/1.1 429 Too Many Requests
Retry-After: 60
X-RateLimit-Limit: 300
X-RateLimit-Remaining: 0
```

---

## 환경 변수

| 변수명 | 기본값 | 설명 |
|--------|--------|------|
| `SERVER_PORT` | `8080` | 서버 포트 |
| `JWT_SECRET` | (필수) | JWT 서명 시크릿 키 (Auth 서비스와 동일) |
| `REDIS_HOST` | `localhost` | Redis 호스트 |
| `REDIS_PORT` | `6379` | Redis 포트 |
| `REDIS_PASSWORD` | (없음) | Redis 비밀번호 |
| `EUREKA_URL` | `http://localhost:8761/eureka/` | Eureka 서버 URL |
| `FRONTEND_URL` | `http://localhost:5173` | CORS 허용 프론트엔드 URL |
| `SPRING_PROFILES_ACTIVE` | `prod` | Spring 프로파일 |

---

## 실행 방법

### 1. 로컬 개발 환경

#### 사전 요구사항
- JDK 21+
- Redis 7.x (`docker run -d -p 6379:6379 redis:7-alpine`)
- Eureka Server 실행 중

```bash
# 환경 변수 설정
export JWT_SECRET="your-256bit-secret-key-here-change-in-production"
export REDIS_HOST=localhost
export EUREKA_URL=http://localhost:8761/eureka/

# Gradle Wrapper로 실행
./gradlew bootRun

# 또는 JAR 빌드 후 실행
./gradlew bootJar
java -jar build/libs/api-gateway-*.jar
```

### 2. Docker로 실행

```bash
# 이미지 빌드
docker build -t web3-community/api-gateway:latest .

# 컨테이너 실행
docker run -d \
  --name api-gateway \
  -p 8080:8080 \
  -e JWT_SECRET="your-secret" \
  -e REDIS_HOST=redis \
  -e EUREKA_URL=http://eureka:8761/eureka/ \
  -e FRONTEND_URL=https://your-domain.com \
  web3-community/api-gateway:latest
```

### 3. Docker Compose (전체 스택)

```bash
# 프로젝트 루트에서 실행
docker-compose up -d
```

### 4. 동작 확인

```bash
# 헬스체크
curl http://localhost:8080/actuator/health

# 라우트 목록 확인
curl http://localhost:8080/actuator/gateway/routes

# 인증 없이 auth 서비스 호출 (통과)
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"password"}'

# JWT 토큰으로 보호된 서비스 호출
curl http://localhost:8080/api/v1/users/me \
  -H "Authorization: Bearer eyJhbGci..."
```

---

## 모니터링

### Spring Boot Actuator 엔드포인트

| 엔드포인트 | 설명 |
|-----------|------|
| `GET /actuator/health` | 서비스 상태 확인 |
| `GET /actuator/metrics` | JVM, Netty, 요청 메트릭 |
| `GET /actuator/gateway/routes` | 등록된 라우트 목록 |
| `GET /actuator/gateway/globalfilters` | 글로벌 필터 목록 |

### 로그 추적

모든 요청에 `X-Request-ID`가 부여되어 분산 로그 추적이 가능하다.

```
# 동일한 X-Request-ID로 API Gateway와 downstream 서비스 로그 연결
2026-03-09 12:00:00 [reactor-http-nio-3] INFO  [abc123def456] LoggingFilter - [req-start] POST /api/v1/boards
2026-03-09 12:00:00 [reactor-http-nio-3] INFO  [abc123def456] LoggingFilter - [req-end]   POST /api/v1/boards | status=201 | elapsed=87ms
```
