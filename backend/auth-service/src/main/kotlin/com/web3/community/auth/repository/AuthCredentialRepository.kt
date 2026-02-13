package com.web3.community.auth.repository

import com.web3.community.auth.entity.AuthCredential
import com.web3.community.auth.entity.AuthProvider
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface AuthCredentialRepository : JpaRepository<AuthCredential, Long> {
    fun findByEmail(email: String): Optional<AuthCredential>
    fun existsByEmail(email: String): Boolean
    fun findByUserId(userId: Long): Optional<AuthCredential>
    fun findByProviderAndProviderId(provider: AuthProvider, providerId: String): Optional<AuthCredential>
}
