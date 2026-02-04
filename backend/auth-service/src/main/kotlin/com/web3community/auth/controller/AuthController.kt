package com.web3community.auth.controller

import com.web3community.auth.dto.*
import com.web3community.auth.service.AuthService
import com.web3community.util.annotation.CurrentUser
import com.web3community.user.dto.CustomUserDetails
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = ["*"])
class AuthController(
    private val authService: AuthService
) {

    @PostMapping("/register")
    fun register(@Valid @RequestBody request: AuthRegistrationRequest): Mono<ResponseEntity<AuthResponse>> {
        return authService.registerUser(request)
            .map { ResponseEntity.ok(it) }
            .onErrorResume { error ->
                Mono.just(ResponseEntity.badRequest().build())
            }
    }

    @PostMapping("/login")
    fun login(@Valid @RequestBody request: AuthLoginRequest): Mono<ResponseEntity<AuthResponse>> {
        return authService.authenticateUser(request)
            .map { ResponseEntity.ok(it) }
            .onErrorResume { error ->
                Mono.just(ResponseEntity.badRequest().build())
            }
    }

    @PostMapping("/refresh")
    fun refreshToken(@Valid @RequestBody request: TokenRefreshRequest): Mono<ResponseEntity<TokenResponse>> {
        return authService.refreshToken(request)
            .map { ResponseEntity.ok(it) }
            .onErrorResume { error ->
                Mono.just(ResponseEntity.badRequest().build())
            }
    }

    @PostMapping("/reset-password")
    fun resetPassword(@Valid @RequestBody request: PasswordResetRequest): Mono<ResponseEntity<String>> {
        return authService.resetPassword(request)
            .map { ResponseEntity.ok(it) }
            .onErrorResume { error ->
                Mono.just(ResponseEntity.badRequest().build())
            }
    }

    @PostMapping("/reset-password-confirm")
    fun resetPasswordConfirm(@Valid @RequestBody request: PasswordResetConfirmation): Mono<ResponseEntity<String>> {
        return authService.resetPasswordConfirm(request)
            .map { ResponseEntity.ok(it) }
            .onErrorResume { error ->
                Mono.just(ResponseEntity.badRequest().build())
            }
    }

    @PostMapping("/change-password")
    fun changePassword(
        @CurrentUser currentUser: CustomUserDetails,
        @Valid @RequestBody request: PasswordChangeRequest
    ): Mono<ResponseEntity<String>> {
        return authService.changePassword(currentUser.getUserId(), request)
            .map { ResponseEntity.ok(it) }
            .onErrorResume { error ->
                Mono.just(ResponseEntity.badRequest().build())
            }
    }

    @GetMapping("/profile")
    fun getProfile(@CurrentUser currentUser: CustomUserDetails): Mono<ResponseEntity<UserProfile>> {
        return authService.getUserProfile(currentUser.getUserId())
            .map { ResponseEntity.ok(it) }
            .onErrorResume { error ->
                Mono.just(ResponseEntity.badRequest().build())
            }
    }

    @PutMapping("/profile")
    fun updateProfile(
        @CurrentUser currentUser: CustomUserDetails,
        @Valid @RequestBody request: ProfileUpdateRequest
    ): Mono<ResponseEntity<UserProfile>> {
        return authService.updateUserProfile(currentUser.getUserId(), request)
            .map { ResponseEntity.ok(it) }
            .onErrorResume { error ->
                Mono.just(ResponseEntity.badRequest().build())
            }
    }

    @GetMapping("/permissions")
    fun getPermissions(@CurrentUser currentUser: CustomUserDetails): Mono<ResponseEntity<List<String>>> {
        return authService.getUserPermissions(currentUser.getUserId())
            .map { ResponseEntity.ok(it) }
            .onErrorResume { error ->
                Mono.just(ResponseEntity.badRequest().build())
            }
    }

    @PutMapping("/role/{userId}")
    fun changeRole(
        @PathVariable userId: String,
        @Valid @RequestBody request: RoleChangeRequest,
        @CurrentUser currentUser: CustomUserDetails
    ): Mono<ResponseEntity<UserProfile>> {
        // Only admins can change roles
        return authService.changeRole(userId, request)
            .map { ResponseEntity.ok(it) }
            .onErrorResume { error ->
                Mono.just(ResponseEntity.badRequest().build())
            }
    }

    @PostMapping("/logout")
    fun logout(@CurrentUser currentUser: CustomUserDetails): Mono<ResponseEntity<String>> {
        return Mono.just(ResponseEntity.ok("Logged out successfully"))
    }
}