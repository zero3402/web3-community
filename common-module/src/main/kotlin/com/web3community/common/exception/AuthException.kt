package com.web3community.common.exception

/**
 * AuthException - 인증/인가 도메인 전용 런타임 예외
 *
 * auth-service의 인증 비즈니스 로직에서 발생하는 예외를 표현합니다.
 * [BusinessException]이 [ErrorCode] 기반의 정형화된 예외라면,
 * AuthException은 동적 메시지가 필요한 인증 오류에 사용됩니다.
 *
 * 주요 사용 시나리오:
 * - 이메일 중복 감지 (회원가입)
 * - 이메일/비밀번호 불일치 (로그인)
 * - 유효하지 않거나 만료된 Refresh Token (토큰 갱신)
 * - Refresh Token 재사용 공격 감지
 * - 로그아웃된 사용자의 토큰 사용 시도
 *
 * 예외 처리:
 * - auth-service의 `@RestControllerAdvice`에서 400 Bad Request 또는
 *   401 Unauthorized로 변환하여 응답합니다.
 * - 보안상 구체적인 오류 원인을 노출하지 않도록 메시지를 일반화하여 사용하세요.
 *   (예: "이메일 없음"과 "비밀번호 틀림"을 구분하지 않음 → 사용자 열거 공격 방지)
 *
 * 사용 예시:
 * ```kotlin
 * // 이메일 중복
 * throw AuthException("이미 사용 중인 이메일입니다: $email")
 *
 * // 로그인 실패 (이메일/비밀번호 구분 없이 동일 메시지)
 * throw AuthException("이메일 또는 비밀번호가 올바르지 않습니다.")
 *
 * // 원인 예외 포함
 * throw AuthException("토큰 처리 중 오류가 발생했습니다.", cause = e)
 * ```
 *
 * @param message 사용자 또는 클라이언트에게 전달할 에러 메시지
 * @param cause 원인 예외 (선택, 스택 트레이스 추적용)
 */
class AuthException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
