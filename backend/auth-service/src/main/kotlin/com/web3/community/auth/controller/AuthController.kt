package com.web3.community.auth.controller

import com.web3.community.auth.dto.*
import com.web3.community.auth.service.AuthService
import com.web3.community.common.dto.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@Tag(name = "Auth", description = "인증 API")
@RestController
@RequestMapping("/auth")
class AuthController(private val authService: AuthService) {

    @Operation(summary = "회원가입")
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    fun register(@Valid @RequestBody request: RegisterRequest): ApiResponse<LoginResponse> {
        return ApiResponse.success(authService.register(request), "Registration successful")
    }

    @Operation(summary = "로그인")
    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest): ApiResponse<LoginResponse> {
        return ApiResponse.success(authService.login(request), "Login successful")
    }

    @Operation(summary = "소셜 로그인 (Google, Naver)")
    @PostMapping("/oauth/login")
    fun socialLogin(@Valid @RequestBody request: OAuthLoginRequest): ApiResponse<LoginResponse> {
        return ApiResponse.success(authService.socialLogin(request), "Social login successful")
    }

    @Operation(summary = "토큰 갱신")
    @PostMapping("/refresh")
    fun refresh(@Valid @RequestBody request: TokenRefreshRequest): ApiResponse<LoginResponse> {
        return ApiResponse.success(authService.refresh(request), "Token refreshed")
    }

    @Operation(summary = "로그아웃")
    @PostMapping("/logout")
    fun logout(@RequestHeader("Authorization") token: String): ApiResponse<Nothing> {
        authService.logout(token)
        return ApiResponse.success("Logout successful")
    }

    @Operation(summary = "비밀번호 변경")
    @PostMapping("/password/change")
    fun changePassword(
        @RequestHeader("X-User-Id") userId: Long,
        @Valid @RequestBody request: PasswordChangeRequest
    ): ApiResponse<Nothing> {
        authService.changePassword(userId, request)
        return ApiResponse.success("Password changed successfully")
    }

    @Operation(summary = "토큰 검증")
    @GetMapping("/validate")
    fun validateToken(@RequestHeader("Authorization") token: String): ApiResponse<Map<String, Any>> {
        return ApiResponse.success(authService.validateToken(token))
    }
}
