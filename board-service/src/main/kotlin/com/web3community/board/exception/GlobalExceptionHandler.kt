package com.web3community.board.exception

import com.web3community.common.exception.BusinessException
import com.web3community.common.response.ApiResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingRequestHeaderException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

/**
 * 전역 예외 처리기
 *
 * 모든 컨트롤러에서 발생하는 예외를 일관된 ApiResponse 형식으로 변환합니다.
 */
@RestControllerAdvice
class GlobalExceptionHandler {

    private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    /**
     * 비즈니스 로직 예외 처리
     * - BOARD_001: 게시글 없음 (404)
     * - BOARD_002: 권한 없음 (403)
     * - BOARD_009: 삭제된 게시글 (410)
     */
    @ExceptionHandler(BusinessException::class)
    fun handleBusinessException(e: BusinessException): ResponseEntity<ApiResponse<Nothing>> {
        logger.warn("BusinessException: {}", e.message)
        return ResponseEntity
            .status(e.errorCode.status)
            .body(ApiResponse.error(e.statusCode(), e.message))
    }

    /**
     * Bean Validation 실패 처리 (@Valid)
     * - 필드 오류 메시지를 쉼표로 연결하여 반환
     */
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(e: MethodArgumentNotValidException): ResponseEntity<ApiResponse<Nothing>> {
        val message = e.bindingResult.fieldErrors
            .joinToString(", ") { "${it.field}: ${it.defaultMessage}" }
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error(400, message))
    }

    /**
     * 필수 요청 헤더 누락 처리
     * - X-User-Id, X-User-Nickname 헤더 없을 때
     */
    @ExceptionHandler(MissingRequestHeaderException::class)
    fun handleMissingHeader(e: MissingRequestHeaderException): ResponseEntity<ApiResponse<Nothing>> {
        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(ApiResponse.error(401, "인증 정보가 없습니다."))
    }

    /**
     * 그 외 예상치 못한 예외 처리
     */
    @ExceptionHandler(Exception::class)
    fun handleException(e: Exception): ResponseEntity<ApiResponse<Nothing>> {
        logger.error("Unexpected error", e)
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.error(500, "서버 내부 오류가 발생했습니다."))
    }
}
