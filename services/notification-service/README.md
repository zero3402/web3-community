# Notification Service

알림 관리 서비스 - 실시간 알림, 이메일, 푸시 알림

## 기능
- 실시간 알림
- 이메일 알림
- 푸시 알림
- 알림 설정 관리
- 알림 이력 조회

## 기술 스택
- Spring Boot 3.2.0
- WebFlux (Reactive)
- MongoDB
- Apache Kafka
- WebSocket

## 실행 방법
```bash
# 도커로 실행
docker build -t notification-service .
docker run -p 8083:8083 notification-service

# 로컬에서 실행
./gradlew bootRun
```

## API 엔드포인트
- GET /api/notifications - 알림 목록 조회
- POST /api/notifications - 알림 생성
- PUT /api/notifications/{id}/read - 알림 읽음 처리
- DELETE /api/notifications/{id} - 알림 삭제

## 환경 변수
- `MONGODB_URI`: MongoDB 연결 URI
- `KAFKA_BOOTSTRAP_SERVERS`: Kafka 브로커 주소