package com.web3.community.user.controller

import com.web3.community.common.dto.ApiResponse
import com.web3.community.common.dto.PageResponse
import com.web3.community.user.dto.CreateUserRequest
import com.web3.community.user.dto.UpdateUserRequest
import com.web3.community.user.dto.UserResponse
import com.web3.community.user.service.UserService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@Tag(name = "User", description = "사용자 API")
@RestController
@RequestMapping("/api/users")
class UserController(private val userService: UserService) {

    @Operation(summary = "사용자 생성")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createUser(@Valid @RequestBody request: CreateUserRequest): ApiResponse<UserResponse> {
        return ApiResponse.success(userService.createUser(request))
    }

    @Operation(summary = "내 프로필 조회")
    @GetMapping("/me")
    fun getMyProfile(@RequestHeader("X-User-Id") userId: Long): ApiResponse<UserResponse> {
        return ApiResponse.success(userService.getUserById(userId))
    }

    @Operation(summary = "내 프로필 수정")
    @PutMapping("/me")
    fun updateMyProfile(
        @RequestHeader("X-User-Id") userId: Long,
        @Valid @RequestBody request: UpdateUserRequest
    ): ApiResponse<UserResponse> {
        return ApiResponse.success(userService.updateUser(userId, request))
    }

    @Operation(summary = "사용자 조회 (ID)")
    @GetMapping("/{id}")
    fun getUserById(@PathVariable id: Long): ApiResponse<UserResponse> {
        return ApiResponse.success(userService.getUserById(id))
    }

    @Operation(summary = "사용자 목록 조회")
    @GetMapping
    fun getAllUsers(@PageableDefault(size = 20) pageable: Pageable): ApiResponse<PageResponse<UserResponse>> {
        return ApiResponse.success(userService.getAllUsers(pageable))
    }

    @Operation(summary = "사용자 역할 변경")
    @PutMapping("/{id}/role")
    fun updateUserRole(
        @PathVariable id: Long,
        @RequestBody roleRequest: Map<String, String>
    ): ApiResponse<UserResponse> {
        val role = roleRequest["role"] ?: throw IllegalArgumentException("Role is required")
        return ApiResponse.success(userService.updateUserRole(id, role))
    }

    @Operation(summary = "사용자 비활성화")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deactivateUser(@PathVariable id: Long) {
        userService.deactivateUser(id)
    }
}
