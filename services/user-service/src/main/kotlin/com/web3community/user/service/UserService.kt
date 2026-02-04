package com.web3community.user.service

import com.web3community.user.dto.UserLoginRequest
import com.web3community.user.dto.UserRegistrationRequest
import com.web3community.user.dto.LoginResponse
import com.web3community.user.dto.UserResponse
import com.web3community.user.entity.User
import com.web3community.user.repository.UserRepository
import com.web3community.util.JwtUtil
import com.web3community.util.PasswordEncoderUtil
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class UserService(
    private val userRepository: UserRepository,
    private val jwtUtil: JwtUtil,
    private val passwordEncoder: PasswordEncoderUtil
) {

    fun registerUser(request: UserRegistrationRequest): LoginResponse {
        if (userRepository.existsByUsername(request.username)) {
            throw IllegalArgumentException("Username already exists")
        }
        
        if (userRepository.existsByEmail(request.email)) {
            throw IllegalArgumentException("Email already exists")
        }

        val encodedPassword = passwordEncoder.encode(request.password)
        val user = User(
            username = request.username,
            email = request.email,
            password = encodedPassword,
            displayName = request.displayName,
            role = request.role
        )

        val savedUser = userRepository.save(user)
        val token = jwtUtil.generateToken(savedUser.email)
        
        return LoginResponse(
            token = token,
            user = UserResponse(
                id = savedUser.id,
                username = savedUser.username,
                email = savedUser.email,
                displayName = savedUser.displayName,
                role = savedUser.role,
                isActive = savedUser.isActive,
                createdAt = savedUser.createdAt.toString()
            )
        )
    }

    fun loginUser(request: UserLoginRequest): LoginResponse {
        val user = userRepository.findByEmail(request.email)
            .orElseThrow { IllegalArgumentException("Invalid credentials") }

        if (!passwordEncoder.matches(request.password, user.password)) {
            throw IllegalArgumentException("Invalid credentials")
        }

        val token = jwtUtil.generateToken(user.email)
        
        return LoginResponse(
            token = token,
            user = UserResponse(
                id = user.id,
                username = user.username,
                email = user.email,
                displayName = user.displayName,
                role = user.role,
                isActive = user.isActive,
                createdAt = user.createdAt.toString()
            )
        )
    }

    fun getUserById(id: Long): UserResponse {
        val user = userRepository.findById(id)
            .orElseThrow { IllegalArgumentException("User not found") }

        return UserResponse(
            id = user.id,
            username = user.username,
            email = user.email,
            displayName = user.displayName,
            role = user.role,
            isActive = user.isActive,
            createdAt = user.createdAt.toString()
        )
    }
}