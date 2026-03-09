package com.web3community.common.security

/**
 * JwtClaims - JWT 토큰 페이로드 데이터 클래스
 *
 * JWT 토큰의 클레임(payload)에 포함되는 사용자 식별 정보입니다.
 * JwtProvider에서 토큰 생성 시 클레임으로 삽입되고,
 * 토큰 파싱 시 이 객체로 역직렬화됩니다.
 *
 * 주의사항:
 * - JWT 페이로드는 암호화되지 않으므로 민감한 정보(비밀번호, 프라이빗 키 등)는 절대 포함 금지
 * - 클레임 크기는 최소화 (네트워크 오버헤드 감소)
 * - 빠르게 변경되는 정보(잔액, 포인트 등)는 포함하지 않음 (토큰 갱신 없이는 반영 불가)
 *
 * JWT 페이로드 예시:
 * ```json
 * {
 *   "userId": 1234,
 *   "email": "user@example.com",
 *   "roles": ["ROLE_USER"],
 *   "iat": 1710000000,
 *   "exp": 1710000900
 * }
 * ```
 *
 * @property userId 사용자 고유 식별자 (DB의 PK)
 * @property email 사용자 이메일 주소 (로그 추적, 알림 발송 등에 사용)
 * @property roles 사용자 권한 목록 (예: ["ROLE_USER", "ROLE_ADMIN"])
 */
data class JwtClaims(
    val userId: Long,
    val email: String,
    val roles: List<String>
)
