package com.web3community.user.controller

import com.web3community.common.response.ApiResponse
import com.web3community.user.dto.UserResponse
import com.web3community.user.dto.UserUpdateRequest
import com.web3community.user.service.UserService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * UserController - 사용자 공개 API 컨트롤러
 *
 * 인증된 사용자를 위한 프로필 조회/수정 API를 제공한다.
 * API Gateway가 JWT 검증 후 X-User-Id 헤더를 주입하므로
 * 이 컨트롤러는 별도의 인증 처리 없이 헤더를 신뢰한다.
 *
 * 기본 경로: /users
 *
 * API 목록:
 * - GET  /users/me          : 내 프로필 조회 (인증 필요)
 * - PUT  /users/me          : 내 프로필 수정 (인증 필요)
 * - GET  /users/{id}        : 특정 사용자 공개 프로필 조회 (인증 불필요)
 *
 * 헤더 규약:
 * - X-User-Id: API Gateway가 JWT sub(UUID)를 DB PK(Long)로 변환하여 주입
 *   (API Gateway에서 auth-service를 통해 변환하거나, JWT에 db-id 클레임을 포함)
 *
 * 보안:
 * - Spring Security를 사용하지 않는다.
 *   API Gateway 레벨에서 인증이 처리되므로 서비스 내부에서는 헤더를 신뢰한다.
 * - CSRF, 세션 관리 불필요 (API Gateway 게이트웨이 패턴).
 */
@RestController
@RequestMapping("/users")
class UserController(
    private val userService: UserService,
) {

    /**
     * 내 프로필 조회
     *
     * API Gateway가 주입한 X-User-Id 헤더로 현재 인증된 사용자의 프로필을 반환한다.
     *
     * 요청 헤더:
     * - X-User-Id: DB auto-increment PK (Long 문자열)
     *
     * 응답:
     * ```json
     * {
     *   "success": true,
     *   "code": 200,
     *   "message": "내 프로필 조회 성공",
     *   "data": {
     *     "id": 1,
     *     "externalId": "550e8400-e29b-41d4-a716-446655440000",
     *     "email": "user@example.com",
     *     "nickname": "홍길동",
     *     "provider": "LOCAL",
     *     "profileImageUrl": null,
     *     "status": "ACTIVE",
     *     "createdAt": "2024-01-01T00:00:00"
     *   }
     * }
     * ```
     *
     * @param userIdHeader X-User-Id 헤더 (API Gateway 주입, DB PK Long 문자열)
     * @return 200 OK + [ApiResponse]<[UserResponse]>
     */
    @GetMapping("/me")
    fun getMyProfile(
        @RequestHeader("X-User-Id") userIdHeader: String,
    ): ResponseEntity<ApiResponse<UserResponse>> {
        val userId = userIdHeader.toLong()
        val response = userService.findMe(userId)
        return ResponseEntity.ok(ApiResponse.success(response, "내 프로필 조회 성공"))
    }

    /**
     * 내 프로필 수정
     *
     * API Gateway가 주입한 X-User-Id 헤더로 현재 인증된 사용자의 프로필을 수정한다.
     * 수정 가능 항목: 닉네임, 프로필 이미지 URL
     *
     * 요청 헤더:
     * - X-User-Id: DB auto-increment PK (Long 문자열)
     *
     * 요청 바디:
     * ```json
     * {
     *   "nickname": "새닉네임",
     *   "profileImageUrl": "https://example.com/image.jpg"
     * }
     * ```
     *
     * 에러 케이스:
     * - 닉네임이 이미 사용 중인 경우: 409 Conflict (USER_003)
     * - 닉네임 형식 오류 (2자 미만, 30자 초과): 400 Bad Request
     *
     * @param userIdHeader X-User-Id 헤더 (API Gateway 주입, DB PK Long 문자열)
     * @param request 수정 요청 DTO
     * @return 200 OK + [ApiResponse]<[UserResponse]>
     */
    @PutMapping("/me")
    fun updateMyProfile(
        @RequestHeader("X-User-Id") userIdHeader: String,
        @Valid @RequestBody request: UserUpdateRequest,
    ): ResponseEntity<ApiResponse<UserResponse>> {
        val userId = userIdHeader.toLong()
        val response = userService.updateMe(userId, request)
        return ResponseEntity.ok(ApiResponse.success(response, "프로필 수정 성공"))
    }

    /**
     * 특정 사용자 공개 프로필 조회
     *
     * 다른 사용자의 공개 프로필을 조회한다. 인증 없이 접근 가능하다.
     * 비밀번호 등 민감 정보는 포함하지 않는다.
     *
     * 경로 변수:
     * - id: 조회할 사용자의 DB PK (Long)
     *
     * 에러 케이스:
     * - 존재하지 않는 사용자 ID: 404 Not Found (USER_001)
     *
     * @param id 조회할 사용자의 DB PK (Long)
     * @return 200 OK + [ApiResponse]<[UserResponse]>
     */
    @GetMapping("/{id}")
    fun getUserProfile(
        @PathVariable id: Long,
    ): ResponseEntity<ApiResponse<UserResponse>> {
        val response = userService.findById(id)
        return ResponseEntity.ok(ApiResponse.success(response, "사용자 프로필 조회 성공"))
    }
}
