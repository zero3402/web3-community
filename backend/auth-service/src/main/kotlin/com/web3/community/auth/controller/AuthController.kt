package com.web3.community.auth.controller

import com.web3.community.auth.dto.*
import com.web3.community.auth.service.AuthService
import com.web3.community.common.dto.ApiResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/auth")
class AuthController(private val authService: AuthService) {

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    fun register(@Valid @RequestBody request: RegisterRequest): ApiResponse<LoginResponse> {
        return ApiResponse.success(authService.register(request), "Registration successful")
    }

    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest): ApiResponse<LoginResponse> {
        return ApiResponse.success(authService.login(request), "Login successful")
    }

    @PostMapping("/refresh")
    fun refresh(@Valid @RequestBody request: TokenRefreshRequest): ApiResponse<LoginResponse> {
        return ApiResponse.success(authService.refresh(request), "Token refreshed")
    }

    @PostMapping("/logout")
    fun logout(@RequestHeader("Authorization") token: String): ApiResponse<Nothing> {
        authService.logout(token)
        return ApiResponse.success("Logout successful")
    }

    @PostMapping("/password/change")
    fun changePassword(
        @RequestHeader("X-User-Id") userId: Long,
        @Valid @RequestBody request: PasswordChangeRequest
    ): ApiResponse<Nothing> {
        authService.changePassword(userId, request)
        return ApiResponse.success("Password changed successfully")
    }

    @GetMapping("/validate")
    fun validateToken(@RequestHeader("Authorization") token: String): ApiResponse<Map<String, Any>> {
        return ApiResponse.success(authService.validateToken(token))
    }
}
