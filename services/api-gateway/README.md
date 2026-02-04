# API Gateway

API 게이트웨이 - 라우팅, 인증, 보안, 로드 밸런싱

## 기능
- 요청 라우팅
- 인증/인가
- CORS 처리
- 속도 제한
- 서킷 브레이커

## 기술 스택
- Spring Cloud Gateway
- WebFlux (Reactive)
- Redis (세션 관리)
- Resilience4j

## 실행 방법
```bash
# 도커로 실행
docker build -t api-gateway .
docker run -p 8080:8080 api-gateway

# 로컬에서 실행
./gradlew bootRun
```

## 라우팅 규칙
- `/api/users/**`, `/api/auth/**` → user-service (8081)
- `/api/posts/**` → post-service (8082)
- `/api/notifications/**` → notification-service (8083)

## 환경 변수
- `REDIS_HOST`: Redis 호스트