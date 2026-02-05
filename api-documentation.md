# =============================================================================
# 🚀 FastAPI Backend API Documentation
# =============================================================================
# RESTful API 서비스 with OpenAPI/Swagger Documentation
# 설명: Web3 Community Platform 백엔드 API 문서화
# 특징: 자동 문서 생성, 타입 안정성, 데이터 검증
# 목적: 프론트엔드-백엔드 통신 명세 제공

# =============================================================================
# 📦 패키지 의존성
# =============================================================================
version: '1.0.0'
description: 'Web3 Community Platform Backend API'

# FastAPI Core
fastapi==0.104.0
uvicorn[standard]==0.23.0
pydantic==2.3.0
pydantic-settings==2.0.0

# Database
sqlalchemy==2.0.0
asyncpg==0.29.0
alembic==1.12.0
databases[postgresql]==0.8.0

# Authentication & Security
python-jose[cryptography]==3.3.0
passlib[bcrypt]==1.7.4
python-multipart==0.0.6

# HTTP Client & API
httpx==0.25.0
aiohttp==3.9.0

# Validation & Serialization
email-validator==2.0.0
python-dateutil==2.8.2

# Monitoring & Metrics
prometheus-client==0.17.0

# Utilities
python-dotenv==1.0.0
pytz==2023.3

# Development & Testing
pytest==7.4.0
pytest-asyncio==0.21.0
pytest-mock==3.12.0
httpx==0.25.0

# Documentation & API
uvicorn[standard]==0.23.0
pydantic==2.3.0

# =============================================================================
# 🌐 API 엔드포인트
# =============================================================================

## 사용자 관리 API (/api/v1/users)

### GET /api/v1/users
# 모든 사용자 목록 조회
# 
# Parameters:
#   - skip (int): 건너뛸 레코드 (default: 0)
#   - limit (int): 반환할 개수 (default: 100)
#   - search (str): 검색어 (optional)
#   - active (bool): 활성 사용자만 필터링 (optional)
# 
# Returns:
#   - 200: 사용자 목록
#   - 422: 검증 오류
# 
# Response Schema:
# {
#   "items": [User],
#   "total": int,
#   "skip": int,
#   "limit": int
# }

### GET /api/v1/users/{user_id}
# 특정 사용자 정보 조회
#
# Parameters:
#   - user_id (str): 사용자 ID
#
# Returns:
#   - 200: 사용자 정보
#   - 404: 사용자 없음

### POST /api/v1/users
# 새 사용자 생성
#
# Request Body:
# {
#   "username": str,
#   "email": str,
#   "password": str,
#   "full_name": str (optional),
#   "is_active": bool (default: true)
# }
#
# Returns:
#   - 201: 생성된 사용자 정보
#   - 400: 잘못된 요청
#   - 422: 검증 오류

### PUT /api/v1/users/{user_id}
# 사용자 정보 수정
#
# Request Body:
# {
#   "username": str (optional),
#   "email": str (optional),
#   "full_name": str (optional),
#   "is_active": bool (optional)
# }
#
# Returns:
#   - 200: 수정된 사용자 정보
#   - 404: 사용자 없음
#   - 422: 검증 오류

### DELETE /api/v1/users/{user_id}
# 사용자 삭제
#
# Returns:
#   - 200: 삭제 성공
#   - 404: 사용자 없음

## 인증 API (/api/v1/auth)

### POST /api/v1/auth/login
# 사용자 로그인
#
# Request Body:
# {
#   "email": str,
#   "password": str
# }
#
# Returns:
#   - 200: 로그인 성공
#     {
#       "access_token": str,
#       "refresh_token": str,
#       "token_type": "bearer",
#       "expires_in": int,
#       "user": User
#     }
#   - 401: 인증 실패
#   - 422: 검증 오류

### POST /api/v1/auth/register
# 사용자 회원가입
#
# Request Body:
# {
#   "username": str,
#   "email": str,
#   "password": str,
#   "full_name": str (optional)
# }
#
# Returns:
#   - 201: 회원가입 성공
#   - 400: 이미 존재하는 사용자
#   - 422: 검증 오류

### POST /api/v1/auth/refresh
# 액세스 토큰 갱신
#
# Request Body:
# {
#   "refresh_token": str
# }
#
# Returns:
#   - 200: 토큰 갱신 성공
#   - 401: 리프레시 토큰 무효

### POST /api/v1/auth/logout
# 사용자 로그아웃
#
# Headers:
#   Authorization: Bearer {access_token}
#
# Returns:
#   - 200: 로그아웃 성공

## 게시글 API (/api/v1/posts)

### GET /api/v1/posts
# 게시글 목록 조회
#
# Parameters:
#   - skip (int): 건너뛸 레코드 (default: 0)
#   - limit (int): 반환할 개수 (default: 20)
#   - category_id (int): 카테고리 ID 필터링 (optional)
#   - author_id (int): 작성자 ID 필터링 (optional)
#   - search (str): 검색어 (optional)
#   - sort_by (str): 정렬 기준 (created_at, updated_at, title)
#   - sort_order (str): 정렬 순서 (asc, desc)
#
# Returns:
#   - 200: 게시글 목록
#   - 422: 검증 오류

### GET /api/v1/posts/{post_id}
# 특정 게시글 조회
#
# Returns:
#   - 200: 게시글 정보
#   - 404: 게시글 없음

### POST /api/v1/posts
# 새 게시글 생성
#
# Headers:
#   Authorization: Bearer {access_token}
#
# Request Body:
# {
#   "title": str,
#   "content": str,
#   "category_id": int,
#   "tags": [str] (optional),
#   "is_published": bool (default: true)
# }
#
# Returns:
#   - 201: 생성된 게시글 정보
#   - 401: 인증 필요
#   - 422: 검증 오류

### PUT /api/v1/posts/{post_id}
# 게시글 수정
#
# Headers:
#   Authorization: Bearer {access_token}
#
# Request Body:
# {
#   "title": str (optional),
#   "content": str (optional),
#   "category_id": int (optional),
#   "tags": [str] (optional),
#   "is_published": bool (optional)
# }
#
# Returns:
#   - 200: 수정된 게시글 정보
#   - 401: 인증 필요
#   - 403: 권한 없음
#   - 404: 게시글 없음
#   - 422: 검증 오류

### DELETE /api/v1/posts/{post_id}
# 게시글 삭제
#
# Headers:
#   Authorization: Bearer {access_token}
#
# Returns:
#   - 200: 삭제 성공
#   - 401: 인증 필요
#   - 403: 권한 없음
#   - 404: 게시글 없음

## 댓글 API (/api/v1/comments)

### GET /api/v1/posts/{post_id}/comments
# 게시글 댓글 목록 조회
#
# Parameters:
#   - skip (int): 건너뛸 레코드 (default: 0)
#   - limit (int): 반환할 개수 (default: 50)
#
# Returns:
#   - 200: 댓글 목록
#   - 404: 게시글 없음

### POST /api/v1/posts/{post_id}/comments
# 새 댓글 생성
#
# Headers:
#   Authorization: Bearer {access_token}
#
# Request Body:
# {
#   "content": str,
#   "parent_id": int (optional, for replies)
# }
#
# Returns:
#   - 201: 생성된 댓글 정보
#   - 401: 인증 필요
#   - 404: 게시글 없음
#   - 422: 검증 오류

### PUT /api/v1/comments/{comment_id}
# 댓글 수정
#
# Headers:
#   Authorization: Bearer {access_token}
#
# Request Body:
# {
#   "content": str
# }
#
# Returns:
#   - 200: 수정된 댓글 정보
#   - 401: 인증 필요
#   - 403: 권한 없음
#   - 404: 댓글 없음
#   - 422: 검증 오류

### DELETE /api/v1/comments/{comment_id}
# 댓글 삭제
#
# Headers:
#   Authorization: Bearer {access_token}
#
# Returns:
#   - 200: 삭제 성공
#   - 401: 인증 필요
#   - 403: 권한 없음
#   - 404: 댓글 없음

## 알림 API (/api/v1/notifications)

### GET /api/v1/notifications
# 사용자 알림 목록 조회
#
# Headers:
#   Authorization: Bearer {access_token}
#
# Parameters:
#   - skip (int): 건너뛸 레코드 (default: 0)
#   - limit (int): 반환할 개수 (default: 50)
#   - unread_only (bool): 읽지 않은 알림만 (default: false)
#
# Returns:
#   - 200: 알림 목록
#   - 401: 인증 필요

### PUT /api/v1/notifications/{notification_id}/read
# 알림 읽음 처리
#
# Headers:
#   Authorization: Bearer {access_token}
#
# Returns:
#   - 200: 처리 성공
#   - 401: 인증 필요
#   - 404: 알림 없음

### PUT /api/v1/notifications/read-all
# 모든 알림 읽음 처리
#
# Headers:
#   Authorization: Bearer {access_token}
#
# Returns:
#   - 200: 처리 성공
#   - 401: 인증 필요

## 파일 업로드 API (/api/v1/files)

### POST /api/v1/files/upload
# 파일 업로드
#
# Headers:
#   Authorization: Bearer {access_token}
#
# Request Body:
#   multipart/form-data
#   - file: UploadFile
#
# Returns:
#   - 200: 업로드된 파일 정보
#     {
#       "id": str,
#       "filename": str,
#       "size": int,
#       "content_type": str,
#       "url": str,
#       "thumbnail_url": str (for images)
#     }
#   - 400: 파일 없음 또는 잘못된 파일 형식
#   - 413: 파일 크기 초과
#   - 422: 검증 오류

## 검색 API (/api/v1/search)

### GET /api/v1/search
# 전체 검색
#
# Parameters:
#   - q (str): 검색어 (required)
#   - type (str): 검색 타입 (all, posts, users, tags) (default: all)
#   - limit (int): 반환할 개수 (default: 20)
#
# Returns:
#   - 200: 검색 결과
#     {
#       "posts": [Post],
#       "users": [User],
#       "tags": [Tag],
#       "total": int
#     }
#   - 400: 검색어 없음
#   - 422: 검증 오류

## 분석 API (/api/v1/analytics)

### GET /api/v1/analytics/overview
# 시스템 개요 데이터
#
# Headers:
#   Authorization: Bearer {access_token}
#   (requires: admin role)
#
# Parameters:
#   - period (str): 기간 (7d, 30d, 90d, 1y) (default: 30d)
#
# Returns:
#   - 200: 분석 데이터
#     {
#       "users": {
#         "total": int,
#         "new": int,
#         "active": int,
#         "growth_rate": float
#       },
#       "posts": {
#         "total": int,
#         "new": int,
#         "published": int,
#         "growth_rate": float
#       },
#       "comments": {
#         "total": int,
#         "new": int,
#         "growth_rate": float
#       },
#       "engagement": {
#         "likes": int,
#         "shares": int,
#         "views": int
#       }
#     }
#   - 401: 인증 필요
#   - 403: 관리자 권한 필요

### GET /api/v1/analytics/users
# 사용자 분석 데이터
#
# Headers:
#   Authorization: Bearer {access_token}
#   (requires: admin role)
#
# Parameters:
#   - period (str): 기간 (7d, 30d, 90d, 1y) (default: 30d)
#   - group_by (str): 그룹핁 기준 (day, week, month) (default: day)
#
# Returns:
#   - 200: 사용자 분석 데이터
#   - 401: 인증 필요
#   - 403: 관리자 권한 필요

## 웹소켓 API (/api/v1/ws)

### WebSocket: /api/v1/ws
# 실시간 통신
#
# Authentication: Query parameter token
#   ?token={access_token}
#
# Message Formats:
# 
# Client -> Server:
# {
#   "type": "subscribe",
#   "channel": "notifications",
#   "user_id": str (optional)
# }
#
# Server -> Client:
# {
#   "type": "notification",
#   "data": {
#     "id": str,
#     "title": str,
#     "message": str,
#     "created_at": str
#   }
# }
#
# {
#   "type": "user_status",
#   "data": {
#     "user_id": str,
#     "status": "online|offline",
#     "last_seen": str
#   }
# }
#
# {
#   "type": "post_update",
#   "data": {
#     "post_id": str,
#     "action": "created|updated|deleted",
#     "post": Post
#   }
# }

## 카테고리 API (/api/v1/categories)

### GET /api/v1/categories
# 카테고리 목록 조회
#
# Returns:
#   - 200: 카테고리 목록
#     [{
#       "id": int,
#       "name": str,
#       "description": str,
#       "post_count": int,
#       "created_at": str
#     }]

### POST /api/v1/categories
# 새 카테고리 생성
#
# Headers:
#   Authorization: Bearer {access_token}
#   (requires: admin role)
#
# Request Body:
# {
#   "name": str,
#   "description": str
# }
#
# Returns:
#   - 201: 생성된 카테고리 정보
#   - 401: 인증 필요
#   - 403: 관리자 권한 필요
#   - 422: 검증 오류

## 시스템 관리 API (/api/v1/admin)

### GET /api/v1/admin/health
# 시스템 상태 확인
#
# Headers:
#   Authorization: Bearer {access_token}
#   (requires: admin role)
#
# Returns:
#   - 200: 시스템 상태
#     {
#       "status": "healthy",
#       "timestamp": str,
#       "services": {
#         "database": "healthy",
#         "redis": "healthy",
#         "kafka": "healthy"
#       },
#       "metrics": {
#         "cpu_usage": float,
#         "memory_usage": float,
#         "disk_usage": float
#       }
#     }
#   - 503: 서비스 비정상

### GET /api/v1/admin/logs
# 시스템 로그 조회
#
# Headers:
#   Authorization: Bearer {access_token}
#   (requires: admin role)
#
# Parameters:
#   - level (str): 로그 레벨 (ERROR, WARNING, INFO, DEBUG)
#   - limit (int): 반환할 개수 (default: 100)
#   - service (str): 서비스 이름 필터링
#
# Returns:
#   - 200: 로그 목록
#   - 401: 인증 필요
#   - 403: 관리자 권한 필요

# =============================================================================
# 🚨 에러 응답 형식
# =============================================================================

## 표준 에러 응답
{
  "detail": "에러 메시지",
  "error_code": "ERROR_CODE",
  "timestamp": "2024-01-15T10:30:00Z",
  "path": "/api/v1/users"
}

## 검증 에러 (422)
{
  "detail": [
    {
      "loc": ["body", "email"],
      "msg": "field required",
      "type": "value_error.missing"
    }
  ]
}

## 인증 에러 (401)
{
  "detail": "Not authenticated",
  "error_code": "UNAUTHORIZED",
  "timestamp": "2024-01-15T10:30:00Z"
}

## 권한 에러 (403)
{
  "detail": "Permission denied",
  "error_code": "FORBIDDEN",
  "timestamp": "2024-01-15T10:30:00Z"
}

## 리소스 없음 (404)
{
  "detail": "Resource not found",
  "error_code": "NOT_FOUND",
  "timestamp": "2024-01-15T10:30:00Z"
}

## 서버 에러 (500)
{
  "detail": "Internal server error",
  "error_code": "INTERNAL_ERROR",
  "timestamp": "2024-01-15T10:30:00Z"
}

# =============================================================================
# 🔄 비동기 처리 API
# =============================================================================

## 작업 상태 조회
### GET /api/v1/tasks/{task_id}
# 비동기 작업 상태 조회
#
# Headers:
#   Authorization: Bearer {access_token}
#
# Returns:
#   - 200: 작업 상태
#     {
#       "id": str,
#       "status": "pending|running|completed|failed",
#       "progress": float,
#       "result": dict (completed),
#       "error": str (failed),
#       "created_at": str,
#       "updated_at": str
#     }
#   - 404: 작업 없음

## 작업 생성
### POST /api/v1/tasks
# 비동기 작업 생성
#
# Headers:
#   Authorization: Bearer {access_token}
#
# Request Body:
# {
#   "type": "export_posts|backup_users|send_notifications",
#   "params": dict (optional)
# }
#
# Returns:
#   - 201: 생성된 작업 정보
#   - 422: 검증 오류

# =============================================================================
# 📊 데이터 모델
# =============================================================================

## User Model
{
  "id": str,
  "username": str,
  "email": str,
  "full_name": str,
  "is_active": bool,
  "is_verified": bool,
  "avatar_url": str,
  "bio": str,
  "created_at": str,
  "updated_at": str,
  "last_login": str
}

## Post Model
{
  "id": str,
  "title": str,
  "content": str,
  "author": User,
  "category": Category,
  "tags": [str],
  "is_published": bool,
  "view_count": int,
  "like_count": int,
  "comment_count": int,
  "created_at": str,
  "updated_at": str
}

## Comment Model
{
  "id": str,
  "content": str,
  "author": User,
  "post": Post,
  "parent": Comment (optional),
  "level": int,
  "like_count": int,
  "is_deleted": bool,
  "created_at": str,
  "updated_at": str
}

## Notification Model
{
  "id": str,
  "recipient": User,
  "type": "mention|like|comment|follow|system",
  "title": str,
  "message": str,
  "data": dict,
  "is_read": bool,
  "created_at": str
}

# =============================================================================
# 🌐 API 접근 정보
# =============================================================================

## 기본 URL
- 개발 환경: http://localhost:8000/api/v1
- 운영 환경: https://api.web3community.com/api/v1

## OpenAPI/Swagger UI
- 개발 환경: http://localhost:8000/docs
- ReDoc: http://localhost:8000/redoc

## 속도 제한
- 인증 없음: 100 requests/minute
- 인증 사용자: 1000 requests/minute
- 관리자: 5000 requests/minute

## 페이징
- 기본 페이지 크기: 20
- 최대 페이지 크기: 100
- 자동 페이지 번호 부여

## 필터링 및 정렬
- 날짜 범위 필터링 지원
- 다중 필터 조합 가능
- 다중 정렬 기준 지원

## 검색
- 전문 검색 지원
- 유사도 점수 기반 정렬
- 하이라이트 기능

# =============================================================================
# 🔧 개발 환경 설정
# =============================================================================

## 환경 변수
```
DATABASE_URL=postgresql://user:password@localhost/web3community
SECRET_KEY=your-secret-key
ACCESS_TOKEN_EXPIRE_MINUTES=30
REFRESH_TOKEN_EXPIRE_DAYS=7
ALGORITHM=HS256
```

## 서버 실행
```bash
# 개발 서버
uvicorn main:app --reload --host 0.0.0.0 --port 8000

# 운영 서버
uvicorn main:app --host 0.0.0.0 --port 8000 --workers 4
```

## 데이터베이스 마이그레이션
```bash
# 마이그레이션 생성
alembic revision --autogenerate -m "Add new table"

# 마이그레이션 적용
alembic upgrade head
```

## 테스트
```bash
# 모든 테스트 실행
pytest

# 특정 테스트 파일 실행
pytest tests/test_users.py

# 커버리지 포함 테스트
pytest --cov=. --cov-report=html
```