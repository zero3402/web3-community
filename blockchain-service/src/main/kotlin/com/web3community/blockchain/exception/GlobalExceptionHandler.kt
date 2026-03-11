package com.web3community.blockchain.exception

import com.web3community.common.exception.BusinessException
import com.web3community.common.response.ApiResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.bind.support.WebExchangeBindException
import org.springframework.web.server.ServerWebExchange

/**
 * 전역 예외 처리기 (WebFlux)
 *
 * ## 개요
 * blockchain-service의 모든 컨트롤러에서 발생하는 예외를 일관된 형식으로 처리한다.
 * WebFlux 환경에서는 `@RestControllerAdvice`와 `ResponseEntity`를 함께 사용한다.
 *
 * ## 처리하는 예외 유형
 * | 예외 클래스                  | HTTP 상태          | 설명                          |
 * |-----------------------------|-------------------|-------------------------------|
 * | [BusinessException]         | 에러코드 기반       | 비즈니스 로직 위반              |
 * | [WebExchangeBindException]  | 400 Bad Request   | 요청 유효성 검사 실패 (@Valid)  |
 * | [Exception]                 | 500 Internal Error | 처리되지 않은 예외               |
 *
 * ## WebFlux 예외 처리 특이점
 * - MVC의 `MethodArgumentNotValidException` 대신 `WebExchangeBindException` 사용
 * - `@ControllerAdvice`는 WebFlux에서도 동작하나 `@RestControllerAdvice`가 더 명시적
 * - 리액티브 스트림 내부 예외는 `onErrorResume`으로 처리해야 하며,
 *   이 핸들러는 컨트롤러에서 직접 throw된 예외만 처리함
 */
@RestControllerAdvice
class GlobalExceptionHandler {

    private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    /**
     * 비즈니스 로직 예외 처리
     *
     * [BusinessException]은 의도적으로 발생시키는 예외로, ErrorCode에 정의된
     * HTTP 상태 코드와 메시지를 사용하여 응답을 생성한다.
     *
     * 예시:
     * - BLOCKCHAIN_002 (409): 지갑 중복 생성
     * - BLOCKCHAIN_003 (404): 지갑 없음
     * - BLOCKCHAIN_004 (400): 잔액 부족
     * - BLOCKCHAIN_007 (503): 브로드캐스트 실패
     *
     * @param ex 발생한 [BusinessException]
     * @param exchange 현재 HTTP 요청/응답 컨텍스트
     * @return ErrorCode에 정의된 HTTP 상태 코드와 메시지를 포함한 응답
     */
    @ExceptionHandler(BusinessException::class)
    fun handleBusinessException(
        ex: BusinessException,
        exchange: ServerWebExchange
    ): ResponseEntity<ApiResponse<Nothing>> {
        logger.warn(
            "[GlobalExceptionHandler] BusinessException: path={}, error={}",
            exchange.request.path,
            ex.toString()
        )

        val status = HttpStatus.valueOf(ex.statusCode())
        return ResponseEntity
            .status(status)
            .body(ApiResponse.error(ex.statusCode(), ex.message))
    }

    /**
     * 요청 바인딩/유효성 검사 예외 처리
     *
     * `@Valid` 어노테이션이 적용된 요청 DTO의 유효성 검사 실패 시 발생한다.
     * WebFlux 환경에서는 [WebExchangeBindException]이 발생한다.
     * (MVC의 `MethodArgumentNotValidException`과 동일한 역할)
     *
     * 응답에 필드별 오류 메시지를 포함하여 클라이언트가 어떤 필드가 잘못되었는지 파악할 수 있게 한다.
     *
     * @param ex 발생한 [WebExchangeBindException]
     * @param exchange 현재 HTTP 요청/응답 컨텍스트
     * @return 400 Bad Request + 필드별 오류 메시지 목록
     */
    @ExceptionHandler(WebExchangeBindException::class)
    fun handleValidationException(
        ex: WebExchangeBindException,
        exchange: ServerWebExchange
    ): ResponseEntity<ApiResponse<Map<String, List<String>>>> {
        // 필드별 오류 메시지를 Map으로 수집
        val fieldErrors = ex.bindingResult
            .fieldErrors
            .groupBy { it.field }
            .mapValues { (_, errors) -> errors.mapNotNull { it.defaultMessage } }

        logger.warn(
            "[GlobalExceptionHandler] 유효성 검사 실패: path={}, errors={}",
            exchange.request.path,
            fieldErrors
        )

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(
                ApiResponse.error(
                    HttpStatus.BAD_REQUEST.value(),
                    "요청 데이터 유효성 검사에 실패했습니다.",
                    fieldErrors
                )
            )
    }

    /**
     * 처리되지 않은 예외 처리 (최후 방어선)
     *
     * 위의 핸들러에서 처리되지 않은 모든 예외를 500 Internal Server Error로 처리한다.
     * 내부 오류 정보(스택 트레이스, 예외 메시지)는 클라이언트에게 노출하지 않는다.
     *
     * @param ex 발생한 [Exception]
     * @param exchange 현재 HTTP 요청/응답 컨텍스트
     * @return 500 Internal Server Error
     */
    @ExceptionHandler(Exception::class)
    fun handleGenericException(
        ex: Exception,
        exchange: ServerWebExchange
    ): ResponseEntity<ApiResponse<Nothing>> {
        logger.error(
            "[GlobalExceptionHandler] 처리되지 않은 예외: path={}, class={}",
            exchange.request.path,
            ex.javaClass.simpleName,
            ex
        )

        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(
                ApiResponse.error(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "서버 내부 오류가 발생했습니다. 잠시 후 다시 시도해 주세요."
                )
            )
    }
}
