package com.web3community.common.exception

/**
 * BusinessException - 비즈니스 로직 예외 클래스
 *
 * 애플리케이션의 비즈니스 규칙 위반 시 발생시키는 커스텀 런타임 예외입니다.
 * ErrorCode를 통해 표준화된 에러 정보를 포함하며, Spring의 전역 예외 처리기
 * (@RestControllerAdvice)에서 일관된 방식으로 처리됩니다.
 *
 * 사용 패턴:
 * 1. 기본 사용 - ErrorCode만 전달
 * ```kotlin
 * val user = userRepository.findById(userId)
 *     ?: throw BusinessException(ErrorCode.USER_001)
 * ```
 *
 * 2. 커스텀 메시지 오버라이드
 * ```kotlin
 * throw BusinessException(ErrorCode.BOARD_001, "ID: $boardId 게시글을 찾을 수 없습니다.")
 * ```
 *
 * 3. 원인 예외 포함 (원인 추적용)
 * ```kotlin
 * try {
 *     cryptoUtils.decrypt(key)
 * } catch (e: Exception) {
 *     throw BusinessException(ErrorCode.BLOCKCHAIN_012, cause = e)
 * }
 * ```
 *
 * @property errorCode 에러 코드 (HTTP 상태 코드, 메시지 포함)
 * @property customMessage 에러 코드의 기본 메시지 대신 사용할 커스텀 메시지 (선택)
 */
class BusinessException(
    val errorCode: ErrorCode,
    private val customMessage: String? = null,
    cause: Throwable? = null
) : RuntimeException(customMessage ?: errorCode.message, cause) {

    /**
     * 실제 에러 메시지 반환
     *
     * 커스텀 메시지가 있으면 커스텀 메시지를, 없으면 ErrorCode에 정의된 메시지를 반환합니다.
     * @return 에러 메시지 문자열
     */
    override val message: String
        get() = customMessage ?: errorCode.message

    /**
     * HTTP 상태 코드 반환
     *
     * Spring의 @ExceptionHandler에서 응답 상태 코드 설정에 사용합니다.
     * @return HTTP 상태 코드 숫자값
     */
    fun statusCode(): Int = errorCode.status.value()

    /**
     * toString 오버라이드 - 로그 출력 시 ErrorCode 정보 포함
     *
     * 로그에서 어떤 에러 코드로 발생한 예외인지 쉽게 확인하기 위해 사용합니다.
     * @return "[BLOCKCHAIN_009] 현재 처리 중인 출금이 있습니다." 형태의 문자열
     */
    override fun toString(): String {
        return "[${errorCode.name}] ${this.message}"
    }
}
