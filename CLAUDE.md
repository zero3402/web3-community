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
| api-gateway | 8080 | Redis | WebFlux | 진행 중 |
| auth-service | 8081 | MySQL + Redis | MVC | 완료 |
| user-service | 8082 | MySQL | MVC | 진행 중 |
| board-service | 8083 | MySQL + Redis | MVC + JPA | 완료 |
| blockchain-service | 8084 | MongoDB | MVC | 진행 중 |

---

## board-service (완료)

### 기술 스택
- Spring Boot Web MVC (Tomcat)
- Spring Data JPA + MySQL (Hibernate)
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
├── dto/                          # 요청/응답 DTO
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
- 반응 토글: 동일 반응 → 취소, 다른 반응 → 변경
- Redis 캐시 키: `board:detail:{id}`, TTL 5분
- Kafka 토픽: board-events (eventType: CREATED/UPDATED/DELETED)
- 권한 검증: X-User-Id 헤더 vs board.authorId

---

## common-module 제공 기능

| 클래스 | 역할 |
|--------|------|
| ApiResponse<T> | 표준 API 응답 래퍼 (success, code, message, data) |
| BusinessException | 비즈니스 예외 (ErrorCode 포함) |
| ErrorCode | 에러 코드 열거형 (AUTH/USER/BOARD/BLOCKCHAIN/COMMON) |
| KafkaTopics | Kafka 토픽 이름 상수 |
| JwtProvider | JWT 발급/검증 |
| CryptoUtils | 암호화 유틸 (블록체인 키 관리) |

---

## 인프라 (docker-compose.yml)

| 서비스 | 포트 | 용도 |
|--------|------|------|
| MySQL 8.0 | 3306 | auth, user, board 서비스 |
| MongoDB 7.0 | 27017 | blockchain 서비스 |
| Redis 7.2 | 6379 | gateway 세션, board 캐시 |
| Kafka | 9092 | 서비스 간 이벤트 |
| Kafka UI | 8080 | 개발용 웹 UI |

---

## 아키텍처 핵심 규칙
- API Gateway: JWT 검증 후 X-User-Id / X-User-Nickname / X-User-Role 헤더로 downstream 전파
- 각 서비스: 헤더 신뢰, 별도 JWT 검증 없음
- 블록체인: UTXO/Nonce 락으로 다중 출금 처리

## 개발 규칙
- 코드 변경 시 이 CLAUDE.md를 항상 최신 상태로 유지
- 새 서비스/기능 추가 시 모듈 아키텍처 테이블 업데이트
- 에러 코드는 common-module ErrorCode 사용
- 모든 코드에 주석 작성
