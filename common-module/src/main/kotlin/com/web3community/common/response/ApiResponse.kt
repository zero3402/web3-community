package com.web3community.common.response

import com.fasterxml.jackson.annotation.JsonInclude

/**
 * ApiResponse - 표준 API 응답 래퍼
 *
 * 모든 API 엔드포인트에서 일관된 응답 형식을 보장하기 위한 제네릭 래퍼 클래스입니다.
 * 성공/실패 여부와 관계없이 항상 동일한 구조로 클라이언트에 반환됩니다.
 *
 * 응답 형식 예시:
 * ```json
 * // 성공
 * {
 *   "success": true,
 *   "code": 200,
 *   "message": "OK",
 *   "data": { "userId": 1, "email": "user@example.com" }
 * }
 *
 * // 실패
 * {
 *   "success": false,
 *   "code": 401,
 *   "message": "인증이 필요합니다.",
 *   "data": null
 * }
 * ```
 *
 * @param T 응답 데이터의 타입 (제네릭)
 * @property success 요청 처리 성공 여부
 * @property code HTTP 상태 코드 (200, 201, 400, 401, 404, 500 등)
 * @property message 응답 메시지 (성공 메시지 또는 에러 설명)
 * @property data 실제 응답 데이터 (null 가능, JSON 직렬화 시 null이면 생략)
 */
@JsonInclude(JsonInclude.Include.NON_NULL) // null 값 필드는 JSON에서 생략
data class ApiResponse<T>(
    val success: Boolean,
    val code: Int,
    val message: String,
    val data: T? = null
) {

    companion object {

        /**
         * 성공 응답 생성 (200 OK, 데이터 포함)
         *
         * @param data 응답에 포함할 데이터
         * @param message 성공 메시지 (기본값: "OK")
         * @return 성공 ApiResponse 인스턴스
         *
         * 사용 예시:
         * ```kotlin
         * return ApiResponse.success(userDto, "사용자 조회 성공")
         * ```
         */
        fun <T> success(data: T, message: String = "OK"): ApiResponse<T> {
            return ApiResponse(
                success = true,
                code = 200,
                message = message,
                data = data
            )
        }

        /**
         * 성공 응답 생성 (지정 HTTP 코드, 데이터 포함)
         *
         * 201 Created 등 200이 아닌 성공 코드에 사용합니다.
         *
         * @param data 응답에 포함할 데이터
         * @param code HTTP 상태 코드
         * @param message 성공 메시지
         * @return 성공 ApiResponse 인스턴스
         *
         * 사용 예시:
         * ```kotlin
         * return ApiResponse.success(createdUser, 201, "사용자 생성 완료")
         * ```
         */
        fun <T> success(data: T, code: Int, message: String): ApiResponse<T> {
            return ApiResponse(
                success = true,
                code = code,
                message = message,
                data = data
            )
        }

        /**
         * 성공 응답 생성 (데이터 없음 - 단순 성공 확인)
         *
         * 삭제 완료, 업데이트 완료 등 반환 데이터가 없는 경우에 사용합니다.
         *
         * @param message 성공 메시지
         * @return 데이터 없는 성공 ApiResponse 인스턴스
         *
         * 사용 예시:
         * ```kotlin
         * return ApiResponse.successNoContent("게시글 삭제 완료")
         * ```
         */
        fun successNoContent(message: String = "OK"): ApiResponse<Nothing> {
            return ApiResponse(
                success = true,
                code = 200,
                message = message,
                data = null
            )
        }

        /**
         * 에러 응답 생성
         *
         * 비즈니스 로직 실패, 유효성 검사 실패, 서버 오류 등에 사용합니다.
         *
         * @param code HTTP 상태 코드 (400, 401, 403, 404, 500 등)
         * @param message 에러 메시지 (클라이언트에게 표시할 내용)
         * @return 에러 ApiResponse 인스턴스
         *
         * 사용 예시:
         * ```kotlin
         * return ApiResponse.error(401, "인증 토큰이 만료되었습니다.")
         * ```
         */
        fun error(code: Int, message: String): ApiResponse<Nothing> {
            return ApiResponse(
                success = false,
                code = code,
                message = message,
                data = null
            )
        }

        /**
         * 에러 응답 생성 (에러 데이터 포함)
         *
         * 유효성 검사 실패 시 어떤 필드에서 에러가 났는지 상세 정보를 포함할 때 사용합니다.
         *
         * @param code HTTP 상태 코드
         * @param message 에러 메시지
         * @param data 에러 상세 데이터 (예: 유효성 검사 오류 목록)
         * @return 에러 데이터 포함 ApiResponse 인스턴스
         */
        fun <T> error(code: Int, message: String, data: T): ApiResponse<T> {
            return ApiResponse(
                success = false,
                code = code,
                message = message,
                data = data
            )
        }
    }
}
