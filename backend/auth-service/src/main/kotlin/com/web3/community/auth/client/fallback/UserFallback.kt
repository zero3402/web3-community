package com.web3.community.auth.client.fallback

import com.web3.community.auth.client.UserClient
import com.web3.community.auth.dto.user.CreateUserRequest
import com.web3.community.auth.dto.user.UserResponse
import com.web3.community.common.dto.ApiResponse
import com.web3.community.common.exception.BusinessException
import com.web3.community.common.exception.ErrorCode
import org.springframework.stereotype.Component

@Component
class UserFallback : UserClient {
    override fun createUserProfile(request: CreateUserRequest): ApiResponse<UserResponse> {
        throw BusinessException(ErrorCode.SYSTEM_BUSY)
    }
}