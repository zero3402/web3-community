# User Service

사용자 관리 서비스 - 사용자 등록, 인증, 프로필 관리

## 기능
- 사용자 CRUD
- JWT 인증
- 비밀번호 암호화
- 이메일 인증
- 소셜 로그인

## 기술 스택
- Spring Boot 3.2.0
- WebFlux (Reactive)
- Spring Security
- JPA/Hibernate
- MySQL
- Redis

## 실행 방법
```bash
# 도커로 실행
docker build -t user-service .
docker run -p 8081:8081 user-service

# 로컬에서 실행
./gradlew bootRun
```

## API 엔드포인트
- POST /api/auth/register - 회원가입
- POST /api/auth/login - 로그인
- GET /api/users/{id} - 사용자 정보 조회
- PUT /api/users/{id} - 사용자 정보 수정
- DELETE /api/users/{id} - 사용자 삭제

## 환경 변수
- `DB_USERNAME`: 데이터베이스 사용자명
- `DB_PASSWORD`: 데이터베이스 비밀번호
- `REDIS_HOST`: Redis 호스트
- `JWT_SECRET`: JWT 서명 키