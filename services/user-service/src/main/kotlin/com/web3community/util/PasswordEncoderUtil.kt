package com.web3community.util

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Component

@Component
class PasswordEncoderUtil {

    private val passwordEncoder = BCryptPasswordEncoder()

    fun encode(rawPassword: String): String {
        return passwordEncoder.encode(rawPassword)
    }

    fun matches(rawPassword: String, encodedPassword: String): Boolean {
        return passwordEncoder.matches(rawPassword, encodedPassword)
    }
}