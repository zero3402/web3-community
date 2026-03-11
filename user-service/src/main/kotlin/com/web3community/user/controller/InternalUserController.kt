package com.web3community.user.controller

import com.web3community.common.response.ApiResponse
import com.web3community.user.dto.InternalUserResponse
import com.web3community.user.service.UserService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * InternalUserController - 서비스 내부 통신 전용 컨트롤러
 *
 * auth-service의 Feign 클라이언트가 호출하는 내부 API를 제공한다.
 * 비밀번호(BCrypt 해시)를 포함한 [InternalUserResponse]를 반환하므로
 * 외부에 절대 노출되어서는 안 된다.
 *
 * 기본 경로: /users/internal
 *
 * API 목록:
 * - GET /users/internal/by-email/{email}              : 이메일로 사용자 조회 (로그인 검증용)
 * - GET /users/internal/by-provider                   : 소셜 제공자+ID로 조회 (OAuth2용)
 *
 * 보안 요구사항:
 * - API Gateway는 `/users/internal/**` 경로를 외부 클라이언트에게 라우팅하지 않아야 한다.
 * - 서비스 메시(Kubernetes NetworkPolicy 또는 서비스 메시)로 내부 트래픽만 허용 권장.
 * - 현재는 경로 기반 격리만 적용 (API Gateway 라우팅 제외).
 *
 * auth-service Feign 클라이언트 연동:
 * ```kotlin
 * // auth-service의 UserServiceClient
 * @FeignClient(name = "user-service", url = "\${user-service.url}")
 * interface UserServiceClient {
 *     @GetMapping("/users/internal/by-email/{email}")
 *     fun findByEmail(@PathVariable email: String): InternalUserResponse?
 *
 *     @GetMapping("/users/internal/by-provider")
 *     fun findByProviderAndProviderId(
 *         @RequestParam provider: String,
 *         @RequestParam providerId: String
 *     ): InternalUserResponse?
 * }
 * ```
 */
@RestController
@RequestMapping("/users/internal")
class InternalUserController(
    private val userService: UserService,
) {

    /**
     * 이메일로 사용자 조회 (auth-service 로그인 검증용)
     *
     * auth-service 로그인 요청 처리 시 Feign으로 호출된다.
     * 반환값에 BCrypt 해시 비밀번호가 포함되어 있어 auth-service가
     * 사용자 입력 비밀번호와 비교 검증하는 데 사용한다.
     *
     * 에러 케이스:
     * - 존재하지 않는 이메일: 404 Not Found (USER_001)
     *   auth-service는 이 오류를 "이메일 또는 비밀번호가 올바르지 않습니다."로 변환하여
     *   사용자 열거 공격(User Enumeration Attack)을 방지한다.
     *
     * 응답 예시:
     * ```json
     * {
     *   "success": true,
     *   "code": 200,
     *   "message": "OK",
     *   "data": {
     *     "id": "550e8400-e29b-41d4-a716-446655440000",
     *     "email": "user@example.com",
     *     "password": "$2a$10$...",
     *     "nickname": "홍길동"
     *   }
     * }
     * ```
     *
     * @param email 조회할 이메일 주소 (URL 경로 변수)
     * @return 200 OK + [ApiResponse]<[InternalUserResponse]> (비밀번호 포함)
     */
    @GetMapping("/by-email/{email}")
    fun findByEmail(
        @PathVariable email: String,
    ): ResponseEntity<ApiResponse<InternalUserResponse>> {
        val response = userService.findByEmail(email)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    /**
     * 소셜 제공자 + 제공자 ID로 사용자 조회 (auth-service OAuth2 기존 회원 확인용)
     *
     * OAuth2 소셜 로그인 시 auth-service가 기존 회원 여부를 확인하기 위해 호출한다.
     * provider + providerId 조합으로 소셜 계정을 고유하게 식별한다.
     *
     * 에러 케이스:
     * - 해당 소셜 계정으로 가입된 사용자 없음: 404 Not Found (USER_001)
     *   auth-service는 이 오류를 신규 회원으로 판단하여 자동 가입 처리한다.
     *
     * @param provider 인증 제공자 ("GOOGLE", "NAVER", "KAKAO")
     * @param providerId 제공자가 부여한 사용자 고유 ID
     * @return 200 OK + [ApiResponse]<[InternalUserResponse]> (비밀번호 포함)
     */
    @GetMapping("/by-provider")
    fun findByProvider(
        @RequestParam provider: String,
        @RequestParam providerId: String,
    ): ResponseEntity<ApiResponse<InternalUserResponse>> {
        val response = userService.findByProviderAndProviderId(provider, providerId)
        return ResponseEntity.ok(ApiResponse.success(response))
    }
}
