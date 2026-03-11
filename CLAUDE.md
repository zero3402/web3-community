# Web3 Community - 프로젝트 컨텍스트

## 프로젝트 개요
- **목표**: Web3 게시판 MSA 서비스
- **기술 스택**: Kotlin + Spring Boot 3.3.0, JDK 21, Vue.js, Kubernetes
- **빌드**: Gradle (Kotlin DSL)

## 기능 목록
- 로그인: 일반 로그인, Google/Naver/Kakao OAuth2 소셜 로그인
- 게시판: 조회/작성/수정/삭제, 좋아요/싫어요 (토글)
- 블록체인 지갑 생성 (BTC, ETH / ERC20), 키 암호화 관리
- 블록체인 전송 (수수료 최적화, 다중 출금 UTXO/Nonce 관리)

---

## 모듈 아키텍처

| 모듈 | 포트 | DB | 프레임워크 | 상태 |
|------|------|----|-----------|------|
| common-module | - | - | - | 완료 |
| api-gateway | 8080 | Redis | WebFlux (Netty) | 완료 |
| auth-service | 8081 | Redis | MVC + OAuth2 | 완료 |
| user-service | 8082 | MySQL | MVC + JPA | 완료 |
| board-service | 8083 | MySQL + Redis | MVC + JPA | 완료 |
| blockchain-service | 8084 | MongoDB | WebFlux + R2DBC | 완료 |

---

## common-module 제공 기능

| 클래스 | 역할 |
|--------|------|
| `ApiResponse<T>` | 표준 API 응답 래퍼 (success, code, message, data) |
| `BusinessException` | 비즈니스 예외 (ErrorCode 포함) |
| `ErrorCode` | 에러 코드 열거형 (AUTH/USER/BOARD/BLOCKCHAIN/COMMON) |
| `KafkaTopics` | Kafka 토픽 이름 상수 (user.created, board-events 등) |
| `JwtUtils` | JWT 발급/검증 (HMAC-SHA256, JJWT 0.12.5) |
| `JwtProvider` | JWT 발급/검증 (RSA 기반, 레거시) |
| `CryptoUtils` | 블록체인 개인키 AES 암호화/복호화 |
| `UserCreatedEvent` | Kafka user.created 이벤트 DTO |
| `AuthException` | 인증 관련 예외 |

---

## api-gateway (완료)

### 기술 스택
- Spring Cloud Gateway (WebFlux/Netty)
- Spring Data Redis (Rate Limiting, 세션)
- Spring Cloud LoadBalancer

### 패키지 구조
```
api-gateway/src/main/kotlin/com/web3community/gateway/
├── ApiGatewayApplication.kt
├── config/
│   ├── GatewayConfig.kt      # IpKeyResolver Bean (Rate Limiting용)
│   └── SecurityConfig.kt
└── filter/
    └── JwtAuthFilter.kt      # JWT 검증 → X-User-Id/Email/Role 헤더 추가
```

### 라우팅 규칙
| 경로 | 대상 | JWT 필요 |
|------|------|----------|
| /api/v1/auth/** | auth-service:8081 | X |
| /api/v1/users/** | user-service:8082 | O |
| /api/v1/boards/** | board-service:8083 | O |
| /api/v1/blockchain/** | blockchain-service:8084 | O |

### 로컬 개발 설정
- `application.yml`: `lb://service-name` (Eureka 사용 시)
- `application-local.yml`: `http://localhost:PORT` (Eureka 없이 직접 연결)
- 로컬 실행 시: `--spring.profiles.active=local` 옵션 추가

### Rate Limiting
- Redis 기반 RequestRateLimiter (IP별 제한)
- 100 req/sec 기본, 200 버스트

---

## auth-service (완료)

### 기술 스택
- Spring Boot Web MVC
- Spring Security + OAuth2 Client
- Spring Data Redis (Refresh Token 저장)
- Spring Kafka Producer (user.created 이벤트 발행)
- OpenFeign (user-service 호출)

### 패키지 구조
```
auth-service/src/main/kotlin/com/web3community/auth/
├── AuthApplication.kt
├── client/UserServiceClient.kt    # Feign: user-service 호출
├── config/
│   ├── SecurityConfig.kt
│   └── RedisConfig.kt
├── controller/
│   ├── AuthController.kt          # /auth/login, /auth/register, /auth/refresh
│   └── OAuth2Controller.kt        # OAuth2 콜백 처리
├── dto/                           # LoginRequest, RegisterRequest, TokenResponse
├── kafka/AuthEventProducer.kt     # user.created 이벤트 발행
├── oauth2/GoogleOAuth2UserService.kt
└── service/
    ├── AuthService.kt             # 로그인/회원가입 로직
    ├── TokenService.kt            # JWT 발급/갱신, Redis Refresh Token 관리
    └── OAuth2Service.kt           # OAuth2 소셜 로그인 처리
```

### API 엔드포인트
- POST /auth/login        - 일반 로그인 (이메일/비밀번호)
- POST /auth/register     - 회원가입
- POST /auth/refresh      - 토큰 갱신
- GET  /auth/oauth2/**    - 소셜 로그인 (Google/Naver/Kakao)

### 핵심 패턴
- auth-service는 자체 DB 없음 → user-service Feign 클라이언트로 사용자 조회
- Refresh Token: Redis DB 0에 저장 (TTL: 7일)
- user.created Kafka 이벤트 → user-service가 소비하여 DB에 저장

---

## user-service (완료)

### 기술 스택
- Spring Boot Web MVC
- Spring Data JPA + MySQL (`web3_user` DB)
- Spring Kafka Consumer (user.created 이벤트 소비)

### 패키지 구조
```
user-service/src/main/kotlin/com/web3community/user/
├── UserApplication.kt             # @EnableJpaAuditing
├── controller/
│   ├── UserController.kt          # /users/me, /users/{id}
│   └── InternalUserController.kt  # /users/internal/by-email/{email}
├── domain/
│   ├── entity/User.kt             # JPA 엔티티 (users 테이블)
│   └── repository/UserRepository.kt
├── dto/
│   ├── UserResponse.kt            # 공개 프로필 (비밀번호 제외)
│   ├── InternalUserResponse.kt    # 내부 용도 (비밀번호 포함, id=externalId)
│   └── UserUpdateRequest.kt       # 닉네임, 프로필 이미지
├── exception/GlobalExceptionHandler.kt
├── kafka/UserEventConsumer.kt     # user.created 소비 → DB 저장
└── service/UserService.kt
```

### 엔티티 설계
- `id: Long` (PK, auto-increment)
- `externalId: String` (UUID, auth-service에서 발급한 사용자 식별자)
- `email`, `password: String?` (소셜 로그인은 null)
- `provider: String` (local/google/naver/kakao)

### API 엔드포인트
- GET  /users/me                       - 내 프로필 (X-User-Id 헤더 필수)
- PUT  /users/me                       - 프로필 수정
- GET  /users/{id}                     - 타 사용자 프로필 조회
- GET  /users/internal/by-email/{email} - 내부 API (auth-service Feign 호출용)

---

## board-service (완료)

### 기술 스택
- Spring Boot Web MVC (Tomcat)
- Spring Data JPA + MySQL (`web3_board` DB)
- Spring Data Redis (StringRedisTemplate, 게시글 캐싱 TTL 5분)
- Apache Kafka Producer (board-events 토픽)

### 패키지 구조
```
board-service/src/main/kotlin/com/web3community/board/
├── BoardApplication.kt           # 진입점 (@EnableJpaAuditing)
├── controller/BoardController.kt # REST API (/boards)
├── domain/
│   ├── entity/Board.kt           # JPA 엔티티 (boards, board_tags 테이블)
│   ├── entity/Reaction.kt        # JPA 엔티티 (reactions 테이블)
│   └── repository/               # JpaRepository 인터페이스
├── dto/                          # BoardResponse, ReactionRequest/Response
├── exception/GlobalExceptionHandler.kt
└── service/BoardService.kt
```

### API 엔드포인트
- GET    /boards                   - 목록 조회 (페이지네이션)
- GET    /boards/{id}              - 상세 조회 (Redis 캐시, 조회수 +1)
- GET    /boards/my                - 내 게시글 목록 (X-User-Id 헤더)
- POST   /boards                   - 게시글 작성
- PUT    /boards/{id}              - 수정 (작성자 본인만)
- DELETE /boards/{id}              - Soft Delete (작성자 본인만)
- POST   /boards/{id}/reactions    - 좋아요/싫어요 토글

### 핵심 패턴
- Soft Delete: `status = DELETED`
- 반응 토글: 동일 반응 → 취소(DELETED), 다른 반응 → 변경(UPDATED), 신규 → 생성(CREATED)
- Redis 캐시 키: `board:detail:{id}`, TTL 5분
- Kafka 토픽: board-events (eventType: CREATED/UPDATED/DELETED)
- 권한 검증: X-User-Id 헤더 vs board.authorId

---

## blockchain-service (완료)

### 기술 스택
- Spring Boot WebFlux (Reactive)
- Spring Data MongoDB Reactive (`web3blockchain` DB)
- Spring Data Redis (UTXO 분산락, ETH Nonce 관리)
- Spring Kafka Producer/Consumer

### 패키지 구조
```
blockchain-service/src/main/kotlin/com/web3community/blockchain/
├── BlockchainApplication.kt
├── config/KafkaConsumerConfig.kt    # withdrawal.batch 컨슈머
├── controller/
│   ├── WalletController.kt          # /wallets/{chain}
│   └── TransactionController.kt     # /transactions/{chain}/send, /history
├── domain/
│   ├── entity/                      # Wallet, TransactionHistory, UtxoSet
│   └── repository/                  # ReactiveMongoRepository 인터페이스
├── dto/                             # WalletResponse, TransactionRequest/Response
├── exception/GlobalExceptionHandler.kt  # WebFlux 전역 예외 처리
└── service/
    ├── WalletService.kt             # BTC/ETH 지갑 생성 (키 암호화)
    ├── EthTransactionService.kt     # ETH 전송 (Nonce 관리)
    ├── BtcTransactionService.kt     # BTC 전송 (UTXO 선택/락)
    └── TransactionMonitorService.kt # PENDING 트랜잭션 모니터링 (@Scheduled)
```

### API 엔드포인트
- POST /wallets/{chain}              - 지갑 생성 (BTC/ETH)
- GET  /wallets                      - 내 지갑 목록
- GET  /wallets/{chain}              - 체인별 지갑 조회
- POST /transactions/{chain}/send    - 트랜잭션 전송
- GET  /transactions/{chain}/history - 트랜잭션 내역

### 핵심 패턴
- BTC: ECKey + P2WPKH Bech32, Greedy UTXO 선택, Redis 분산락 (`utxo:lock:{id}`)
- ETH: Keys.createEcKeyPair + EIP-55, Redis 원자적 Nonce 관리 (`eth:nonce:{address}`)
- 개인키: CryptoUtils.encrypt/decrypt (AES 암호화)

---

## 인프라 (docker-compose.yml)

| 서비스 | 포트 | 용도 |
|--------|------|------|
| MySQL 8.0 | 3306 | user-service (`web3_user`), board-service (`web3_board`) |
| MongoDB 7.0 | 27017 | blockchain-service (`web3blockchain`) |
| Redis 7.2 | 6379 | gateway(DB 0), auth(DB 0), board(DB 1), blockchain 분산락 |
| Zookeeper | 2181 | Kafka 코디네이션 |
| Kafka | 9092 | 서비스 간 이벤트 (user.created, board-events, withdrawal.batch) |
| Kafka UI | 8080 | 개발용 웹 UI |

### MySQL 초기화
- `scripts/mysql/init.sql` 마운트 → `web3_user`, `web3_board` DB 자동 생성

---

## 아키텍처 핵심 규칙
- API Gateway: JWT 검증 후 `X-User-Id` / `X-User-Nickname` / `X-User-Role` 헤더로 downstream 전파
- 각 서비스: 헤더 신뢰, 별도 JWT 검증 없음
- auth-service: 자체 DB 없음, user-service Feign 호출로 사용자 조회
- Kafka: auth-service가 user.created 이벤트 발행 → user-service가 소비하여 DB 저장 (이벤트 소싱)
- Soft Delete: board-service(`BoardStatus.DELETED`), user-service(`UserStatus.INACTIVE`)
- 블록체인: UTXO/Nonce 락으로 다중 동시 출금 처리

## Kafka 토픽

| 토픽 | Producer | Consumer | 용도 |
|------|----------|----------|------|
| user.created | auth-service | user-service | 회원가입 이벤트 |
| board-events | board-service | - | 게시글 CRUD 이벤트 |
| withdrawal.batch | blockchain-service | blockchain-service | 다중 출금 배치 |

## Redis DB 할당

| DB 인덱스 | 서비스 | 용도 |
|-----------|--------|------|
| 0 | api-gateway, auth-service | Rate Limiting, Refresh Token |
| 1 | board-service | 게시글 캐시 (TTL: 5분) |
| 2+ | blockchain-service | UTXO 락, ETH Nonce |

## 개발 규칙
- 코드 변경 시 이 CLAUDE.md를 항상 최신 상태로 유지
- 새 서비스/기능 추가 시 모듈 아키텍처 테이블 업데이트
- 에러 코드는 common-module ErrorCode 사용
- 모든 코드에 주석 작성
- 로컬 개발: `--spring.profiles.active=local` 프로파일 사용
