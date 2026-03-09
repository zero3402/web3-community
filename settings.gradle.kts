/**
 * settings.gradle.kts - Gradle 멀티모듈 루트 설정 파일
 *
 * 이 파일은 Gradle 빌드 시스템에서 프로젝트 구조를 정의합니다.
 * 각 서브모듈은 독립적인 Spring Boot 애플리케이션으로 배포되며,
 * common-module을 공통 의존성으로 공유합니다.
 *
 * 모듈 구조:
 * ├── common-module       - 공통 유틸리티, DTO, 보안 컴포넌트
 * ├── api-gateway         - API 게이트웨이 (WebFlux 기반 라우팅)
 * ├── auth-service        - 인증/인가 서비스 (OAuth2, JWT 발급)
 * ├── user-service        - 사용자 관리 서비스
 * ├── board-service       - 게시판 서비스 (글/댓글/좋아요)
 * └── blockchain-service  - 블록체인 지갑/전송 서비스 (BTC, ETH)
 */

// 루트 프로젝트 이름 설정
rootProject.name = "web3-community"

// ============================================================
// 공통 모듈 - 모든 서비스가 의존하는 공유 라이브러리
// ============================================================
include("common-module")

// ============================================================
// MSA 서비스 모듈들
// 각 모듈은 독립적으로 빌드/배포 가능한 Spring Boot 애플리케이션
// ============================================================

// API 게이트웨이: 외부 요청 라우팅, 인증 필터, 로드밸런싱
include("api-gateway")

// 인증 서비스: 일반 로그인 / OAuth2 소셜 로그인 / JWT 토큰 발급 및 갱신
include("auth-service")

// 사용자 서비스: 회원 정보 CRUD, 프로필 관리
include("user-service")

// 게시판 서비스: 게시글/댓글 CRUD, 좋아요/싫어요 기능
include("board-service")

// 블록체인 서비스: 지갑 생성(BTC/ETH), 전송, 수수료 최적화, 다중 출금
include("blockchain-service")
