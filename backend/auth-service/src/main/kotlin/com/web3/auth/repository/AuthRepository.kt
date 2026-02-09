package com.web3community.auth.repository

import com.web3community.auth.entity.AuthSession
import org.springframework.data.r2dbc.repository.R2dbcRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Mono

@Repository
interface AuthRepository : R2dbcRepository<AuthSession, Long> {
    fun findByToken(token: String): Mono<AuthSession>
    fun findByUserIdAndRefreshToken(userId: Long, refreshToken: String): Mono<AuthSession>
    fun deleteByToken(token: String): Mono<Void>
    fun deleteByUserIdAndRefreshToken(userId: Long, refreshToken: String): Mono<Void>
}