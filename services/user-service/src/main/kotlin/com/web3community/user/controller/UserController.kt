package com.web3community.user.controller

import com.web3community.user.dto.UserLoginRequest
import com.web3community.user.dto.UserRegistrationRequest
import com.web3community.user.dto.LoginResponse
import com.web3community.user.dto.UserResponse
import com.web3community.user.service.UserService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import jakarta.validation.Valid

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = ["*"])
class UserController(private val userService: UserService) {

    @PostMapping("/register")
    fun registerUser(@Valid @RequestBody request: UserRegistrationRequest): ResponseEntity<LoginResponse> {
        return try {
            val response = userService.registerUser(request)
            ResponseEntity.status(HttpStatus.CREATED).body(response)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().build()
        }
    }

    @PostMapping("/login")
    fun loginUser(@Valid @RequestBody request: UserLoginRequest): ResponseEntity<LoginResponse> {
        return try {
            val response = userService.loginUser(request)
            ResponseEntity.ok(response)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        }
    }

    @GetMapping("/users/{id}")
    fun getUserById(@PathVariable id: Long): ResponseEntity<UserResponse> {
        return try {
            val user = userService.getUserById(id)
            ResponseEntity.ok(user)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.notFound().build()
        }
    }
}