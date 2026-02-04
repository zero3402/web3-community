package com.web3community.user.repository

import com.web3community.user.entity.User
import com.web3community.user.dto.UserRole
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface UserRepository : JpaRepository<User, Long> {
    fun findByEmail(email: String): Optional<User>
    fun findByUsername(username: String): Optional<User>
    fun existsByUsername(username: String): Boolean
    fun existsByEmail(email: String): Boolean
    fun findByIsActive(isActive: Boolean): List<User>
    fun findByRole(role: UserRole): List<User>
    fun findByUsernameContainingIgnoreCase(username: String): List<User>
    fun findByEmailContainingIgnoreCase(email: String): List<User>
    
    @Query("SELECT COUNT(u) FROM User u WHERE u.isActive = :isActive")
    fun countByIsActive(@Param("isActive") isActive: Boolean): Long
    
    @Query("SELECT COUNT(u) FROM User u WHERE u.isEmailVerified = :isEmailVerified")
    fun countByIsEmailVerified(@Param("isEmailVerified") isEmailVerified: Boolean): Long
    
    @Query("SELECT COUNT(u) FROM User u WHERE u.role = :role")
    fun countByRole(@Param("role") role: UserRole): Long
    
    fun findAllByOrderByCreatedAtDesc(): List<User>
}