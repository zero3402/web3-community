# Post Service

게시글 관리 서비스 - 게시글 CRUD, 검색, 분류

## 기능
- 게시글 CRUD
- 게시글 검색
- 카테고리 관리
- 태그 기능
- 좋아요 기능

## 기술 스택
- Spring Boot 3.2.0
- WebFlux (Reactive)
- MongoDB
- Validation

## 실행 방법
```bash
# 도커로 실행
docker build -t post-service .
docker run -p 8082:8082 post-service

# 로컬에서 실행
./gradlew bootRun
```

## API 엔드포인트
- GET /api/posts - 게시글 목록 조회
- POST /api/posts - 게시글 생성
- GET /api/posts/{id} - 게시글 상세 조회
- PUT /api/posts/{id} - 게시글 수정
- DELETE /api/posts/{id} - 게시글 삭제

## 환경 변수
- `MONGODB_URI`: MongoDB 연결 URI