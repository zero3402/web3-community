package com.web3community.user.infrastructure.controller

import com.web3community.user.application.service.UserApplicationService
import com.web3community.user.application.dto.*
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.support.ServletUriComponentsBuilder

@RestController
@RequestMapping("/api/v1/users")
@CrossOrigin(origins = ["*"])
class UserController(
    private val userApplicationService: UserApplicationService
) {

    @PostMapping("/register")
    fun registerUser(@Valid @RequestBody request: RegisterUserRequest): ResponseEntity<UserResponse> {
        val user = userApplicationService.registerUser(request)
        
        val location = ServletUriComponentsBuilder
            .fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(user.id)
            .toUri()
        
        return ResponseEntity.created(location).body(user)
    }

    @PostMapping("/login")
    fun loginUser(@Valid @RequestBody request: LoginUserRequest): ResponseEntity<LoginResponse> {
        return ResponseEntity.ok(userApplicationService.loginUser(request))
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('USER') or hasRole('MODERATOR') or hasRole('ADMIN')")
    fun getUserProfile(@PathVariable id: Long): ResponseEntity<UserProfileResponse> {
        return ResponseEntity.ok(userApplicationService.getUserProfile(id))
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('USER') and @userService.isCurrentUser(#id, authentication)")
    fun updateUserProfile(
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateUserProfileRequest
    ): ResponseEntity<UserProfileResponse> {
        return ResponseEntity.ok(userApplicationService.updateUserProfile(id, request))
    }

    @PostMapping("/{id}/change-password")
    @PreAuthorize("hasRole('USER') and @userService.isCurrentUser(#id, authentication)")
    fun changePassword(
        @PathVariable id: Long,
        @Valid @RequestBody request: ChangePasswordRequest
    ): ResponseEntity<Map<String, String>> {
        userApplicationService.changePassword(id, request)
        return ResponseEntity.ok(mapOf("message" to "Password changed successfully"))
    }

    @PostMapping("/{followerId}/follow/{followeeId}")
    @PreAuthorize("hasRole('USER') and @userService.isCurrentUser(#followerId, authentication)")
    fun followUser(
        @PathVariable followerId: Long,
        @PathVariable followeeId: Long
    ): ResponseEntity<FollowResponse> {
        return ResponseEntity.ok(userApplicationService.followUser(followerId, followeeId))
    }

    @DeleteMapping("/{followerId}/follow/{followeeId}")
    @PreAuthorize("hasRole('USER') and @userService.isCurrentUser(#followerId, authentication)")
    fun unfollowUser(
        @PathVariable followerId: Long,
        @PathVariable followeeId: Long
    ): ResponseEntity<FollowResponse> {
        return ResponseEntity.ok(userApplicationService.unfollowUser(followerId, followeeId))
    }

    @GetMapping("/search")
    fun searchUsers(
        @RequestParam query: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<UserListResponse> {
        return ResponseEntity.ok(userApplicationService.searchUsers(query, page, size))
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('USER')")
    fun getCurrentUser(): ResponseEntity<UserProfileResponse> {
        // 현재 인증된 사용자의 ID 가져오기 (구현 필요)
        val currentUserId = getCurrentUserId()
        return ResponseEntity.ok(userApplicationService.getUserProfile(currentUserId))
    }

    private fun getCurrentUserId(): Long {
        // Spring Security 컨텍스트에서 현재 사용자 ID 가져오기
        // 구현 필요
        return 1L // 임시
    }
}