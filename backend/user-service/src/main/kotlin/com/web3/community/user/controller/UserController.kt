package com.web3.community.user.controller

import com.web3.community.common.dto.ApiResponse
import com.web3.community.common.dto.PageResponse
import com.web3.community.user.dto.CreateUserRequest
import com.web3.community.user.dto.UpdateUserRequest
import com.web3.community.user.dto.UserResponse
import com.web3.community.user.service.UserService
import jakarta.validation.Valid
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/users")
class UserController(private val userService: UserService) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createUser(@Valid @RequestBody request: CreateUserRequest): ApiResponse<UserResponse> {
        return ApiResponse.success(userService.createUser(request))
    }

    @GetMapping("/me")
    fun getMyProfile(@RequestHeader("X-User-Id") userId: Long): ApiResponse<UserResponse> {
        return ApiResponse.success(userService.getUserById(userId))
    }

    @PutMapping("/me")
    fun updateMyProfile(
        @RequestHeader("X-User-Id") userId: Long,
        @Valid @RequestBody request: UpdateUserRequest
    ): ApiResponse<UserResponse> {
        return ApiResponse.success(userService.updateUser(userId, request))
    }

    @GetMapping("/{id}")
    fun getUserById(@PathVariable id: Long): ApiResponse<UserResponse> {
        return ApiResponse.success(userService.getUserById(id))
    }

    @GetMapping
    fun getAllUsers(@PageableDefault(size = 20) pageable: Pageable): ApiResponse<PageResponse<UserResponse>> {
        return ApiResponse.success(userService.getAllUsers(pageable))
    }

    @PutMapping("/{id}/role")
    fun updateUserRole(
        @PathVariable id: Long,
        @RequestBody roleRequest: Map<String, String>
    ): ApiResponse<UserResponse> {
        val role = roleRequest["role"] ?: throw IllegalArgumentException("Role is required")
        return ApiResponse.success(userService.updateUserRole(id, role))
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deactivateUser(@PathVariable id: Long) {
        userService.deactivateUser(id)
    }
}
