package com.web3community.user.domain.repository

import com.web3community.user.domain.model.User
import com.web3community.sharedkernel.domain.UserId
import com.web3community.sharedkernel.domain.Email
import com.web3community.sharedkernel.domain.Username
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface UserRepository : JpaRepository<User, Long> {
    
    fun findByEmail(email: Email): Optional<User>
    fun findByUsername(username: Username): Optional<User>
    fun existsByEmail(email: Email): Boolean
    fun existsByUsername(username: Username): Boolean
    
    fun findByIsEmailVerifiedFalse(): List<User>
    fun findByIsActiveTrue(): List<User>
    fun findByRole(role: com.web3community.user.domain.model.UserRole): List<User>
    
    fun findByFollowing_FolloweeId(followeeId: Long): List<User>
    fun findByFollowers_FollowerId(followerId: Long): List<User>
    
    // 커스텀 쿼리 메소드
    @org.springframework.data.jpa.repository.Query("""
        SELECT COUNT(u) FROM User u 
        JOIN u.followers f 
        WHERE f.followee.id = :userId
    """)
    fun countFollowers(@org.springframework.data.repository.query.Param("userId") userId: Long): Long
    
    @org.springframework.data.jpa.repository.Query("""
        SELECT COUNT(u) FROM User u 
        JOIN u.following f 
        WHERE f.follower.id = :userId
    """)
    fun countFollowing(@org.springframework.data.repository.query.Param("userId") userId: Long): Long
    
    fun searchUsers(query: String): List<User>
    fun findUsersByLocation(location: String): List<User>
}