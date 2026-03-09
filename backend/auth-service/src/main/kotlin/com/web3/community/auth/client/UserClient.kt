package com.web3.community.auth.client

import com.web3.community.auth.client.fallback.UserFallback
import com.web3.community.auth.dto.user.CreateUserRequest
import com.web3.community.auth.dto.user.UserResponse
import com.web3.community.common.dto.ApiResponse
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody

@FeignClient(
        name = "user-service",
        url = "\${services.user-service-url:http://localhost:8082}",
        fallback = UserFallback::class
)
interface UserClient {
    @PostMapping("/api/users")
    fun createUserProfile(@RequestBody request: CreateUserRequest): ApiResponse<UserResponse>
}