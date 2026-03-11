package com.web3community.user.exception

import com.web3community.common.exception.BusinessException
import com.web3community.common.response.ApiResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException

/**
 * GlobalExceptionHandler - 전역 예외 처리기
 *
 * @RestControllerAdvice를 사용하여 컨트롤러에서 발생한 모든 예외를
 * 일관된 [ApiResponse] 형식으로 변환하여 클라이언트에 반환한다.
 *
 * 처리 예외 목록:
 * - [BusinessException]: 비즈니스 로직 예외 (USER_001, USER_003 등)
 * - [MethodArgumentNotValidException]: @Valid 유효성 검사 실패
 * - [MethodArgumentTypeMismatchException]: 경로 변수 타입 불일치 (예: String → Long 변환 실패)
 * - [NumberFormatException]: 숫자 파싱 실패 (X-User-Id 헤더 toLong() 실패 등)
 * - [Exception]: 위 케이스에 해당하지 않는 예상치 못한 예외
 *
 * 로깅 정책:
 * - BusinessException: WARN 레벨 (예상된 비즈니스 오류)
 * - MethodArgumentNotValidException: DEBUG 레벨 (클라이언트 입력 오류)
 * - 그 외: ERROR 레벨 (예상치 못한 서버 오류)
 */
@RestControllerAdvice
class GlobalExceptionHandler {

    private val log = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    /**
     * 비즈니스 로직 예외 처리
     *
     * 서비스 레이어에서 발생한 [BusinessException]을 처리한다.
     * ErrorCode에 정의된 HTTP 상태 코드와 메시지를 그대로 사용한다.
     *
     * 예시 응답 (USER_001):
     * ```json
     * { "success": false, "code": 404, "message": "사용자를 찾을 수 없습니다." }
     * ```
     *
     * @param e 발생한 BusinessException
     * @return ErrorCode의 HTTP 상태 코드와 메시지를 담은 ResponseEntity
     */
    @ExceptionHandler(BusinessException::class)
    fun handleBusinessException(e: BusinessException): ResponseEntity<ApiResponse<Nothing>> {
        log.warn("BusinessException 발생: [{}] {}", e.errorCode.name, e.message)
        return ResponseEntity
            .status(e.errorCode.status)
            .body(ApiResponse.error(e.statusCode(), e.message))
    }

    /**
     * Bean Validation 유효성 검사 실패 처리
     *
     * @Valid 어노테이션이 적용된 요청 바디의 유효성 검사 실패 시 발생한다.
     * 모든 필드 오류를 수집하여 상세한 에러 정보를 반환한다.
     *
     * 예시 응답:
     * ```json
     * {
     *   "success": false,
     *   "code": 400,
     *   "message": "유효성 검사 실패",
     *   "data": { "nickname": "닉네임은 2자 이상 30자 이하여야 합니다." }
     * }
     * ```
     *
     * @param e 발생한 MethodArgumentNotValidException
     * @return 400 Bad Request + 필드별 오류 메시지 맵
     */
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(
        e: MethodArgumentNotValidException,
    ): ResponseEntity<ApiResponse<Map<String, String>>> {
        // 모든 필드 오류를 맵으로 수집 (필드명 → 오류 메시지)
        val errors = e.bindingResult.allErrors.associate { error ->
            val fieldName = (error as? FieldError)?.field ?: "unknown"
            val message = error.defaultMessage ?: "유효하지 않은 값입니다."
            fieldName to message
        }

        log.debug("유효성 검사 실패: {}", errors)
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error(HttpStatus.BAD_REQUEST.value(), "유효성 검사 실패", errors))
    }

    /**
     * 경로 변수 타입 불일치 예외 처리
     *
     * @PathVariable에 잘못된 타입의 값이 전달될 때 발생한다.
     * 예: GET /users/abc → {id} 파라미터에 Long 변환 실패
     *
     * 예시 응답:
     * ```json
     * { "success": false, "code": 400, "message": "잘못된 요청입니다. 요청 파라미터를 확인해 주세요." }
     * ```
     *
     * @param e 발생한 MethodArgumentTypeMismatchException
     * @return 400 Bad Request
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleTypeMismatchException(
        e: MethodArgumentTypeMismatchException,
    ): ResponseEntity<ApiResponse<Nothing>> {
        log.debug("파라미터 타입 불일치: paramName={}, value={}", e.name, e.value)
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error(HttpStatus.BAD_REQUEST.value(), "잘못된 요청입니다. 요청 파라미터를 확인해 주세요."))
    }

    /**
     * 숫자 파싱 예외 처리
     *
     * X-User-Id 헤더 값을 Long으로 변환(toLong())하거나,
     * 문자열을 숫자로 파싱할 때 형식이 잘못된 경우 발생한다.
     *
     * 예시 응답:
     * ```json
     * { "success": false, "code": 400, "message": "잘못된 요청입니다. 요청 파라미터를 확인해 주세요." }
     * ```
     *
     * @param e 발생한 NumberFormatException
     * @return 400 Bad Request
     */
    @ExceptionHandler(NumberFormatException::class)
    fun handleNumberFormatException(
        e: NumberFormatException,
    ): ResponseEntity<ApiResponse<Nothing>> {
        log.debug("숫자 파싱 실패: {}", e.message)
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error(HttpStatus.BAD_REQUEST.value(), "잘못된 요청입니다. 요청 파라미터를 확인해 주세요."))
    }

    /**
     * 예상치 못한 예외 처리 (Catch-all)
     *
     * 위 핸들러에서 처리되지 않은 모든 예외를 잡아 500 Internal Server Error를 반환한다.
     * 실제 예외 정보는 서버 로그에만 기록하고 클라이언트에는 일반적인 메시지만 반환한다.
     * (내부 구현 상세 노출 방지)
     *
     * @param e 발생한 예외
     * @return 500 Internal Server Error
     */
    @ExceptionHandler(Exception::class)
    fun handleUnexpectedException(e: Exception): ResponseEntity<ApiResponse<Nothing>> {
        log.error("예상치 못한 예외 발생: {}", e.message, e)
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "서버 내부 오류가 발생했습니다. 잠시 후 다시 시도해 주세요."))
    }
}
