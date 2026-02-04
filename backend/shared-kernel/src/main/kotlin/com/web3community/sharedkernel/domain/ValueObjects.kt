package com.web3community.sharedkernel.domain

import jakarta.persistence.Embeddable
import jakarta.persistence.Embeddable
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import java.util.*

// 이메일 Value Object
@JvmInlineValue
value class Email(val value: String) {
    init {
        require(value.isNotBlank()) { "Email cannot be blank" }
        require(isValidEmail(value)) { "Invalid email format" }
    }
    
    companion object {
        fun of(value: String): Email = Email(value)
        
        private fun isValidEmail(email: String): Boolean {
            val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
            return emailRegex.toRegex().matches(email)
        }
    }
    
    fun domain(): String = value.split("@").last()
}

// 사용자명 Value Object
@JvmInlineValue
value class Username(val value: String) {
    init {
        require(value.isNotBlank()) { "Username cannot be blank" }
        require(value.length in 3..30) { "Username must be between 3 and 30 characters" }
        require(isValidUsername(value)) { "Username contains invalid characters" }
    }
    
    companion object {
        fun of(value: String): Username = Username(value)
        
        private fun isValidUsername(username: String): Boolean {
            val usernameRegex = "^[a-zA-Z0-9_-]+$"
            return usernameRegex.toRegex().matches(username)
        }
    }
}

// 비밀번호 Value Object
@JvmInlineValue
value class Password(val value: String) {
    init {
        require(value.length >= 8) { "Password must be at least 8 characters" }
        require(hasUpperCase(value)) { "Password must contain at least one uppercase letter" }
        require(hasLowerCase(value)) { "Password must contain at least one lowercase letter" }
        require(hasDigit(value)) { "Password must contain at least one digit" }
    }
    
    companion object {
        fun of(value: String): Password = Password(value)
        
        private fun hasUpperCase(password: String): Boolean = password.any { it.isUpperCase() }
        private fun hasLowerCase(password: String): Boolean = password.any { it.isLowerCase() }
        private fun hasDigit(password: String): Boolean = password.any { it.isDigit() }
    }
}

// URL Value Object
@JvmInlineValue
value class Url(val value: String) {
    init {
        require(value.isNotBlank()) { "URL cannot be blank" }
        require(isValidUrl(value)) { "Invalid URL format" }
    }
    
    companion object {
        fun of(value: String): Url = Url(value)
        
        private fun isValidUrl(url: String): Boolean {
            return try {
                java.net.URL(url).toURI()
                true
            } catch (e: Exception) {
                false
            }
        }
    }
}

// 전화번호 Value Object
@JvmInlineValue
value class PhoneNumber(val value: String) {
    init {
        require(isValidPhoneNumber(value)) { "Invalid phone number format" }
    }
    
    companion object {
        fun of(value: String): PhoneNumber = PhoneNumber(value)
        
        private fun isValidPhoneNumber(phone: String): Boolean {
            val phoneRegex = "^\\+?[0-9]{10,15}$"
            return phoneRegex.toRegex().matches(phone.replace("[^0-9+]".toRegex(), ""))
        }
    }
}