package com.web3community.auth.service

import org.springframework.security.core.userdetails.ReactiveUserDetailsService
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service
import com.web3community.user.repository.UserRepository
import org.springframework.security.core.authority.SimpleGrantedAuthority
import reactor.core.publisher.Mono

@Service
class ReactiveUserDetailsService(
    private val userRepository: UserRepository
) : ReactiveUserDetailsService {

    override fun findByUsername(username: String): Mono<UserDetails> {
        return userRepository.findByEmail(username)
            .map { user ->
                org.springframework.security.core.userdetails.User.builder()
                    .username(user.email)
                    .password(user.password)
                    .authorities(
                        SimpleGrantedAuthority("ROLE_${user.role.name}"),
                        *getPermissionsForRole(user.role).map { SimpleGrantedAuthority(it) }.toTypedArray()
                    )
                    .accountExpired(!user.isActive)
                    .accountLocked(!user.isActive)
                    .credentialsExpired(false)
                    .disabled(!user.isActive)
                    .build()
            }
            .switchIfEmpty(Mono.error(UsernameNotFoundException("User not found: $username")))
    }

    private fun getPermissionsForRole(role: com.web3community.user.dto.AuthRole): List<String> {
        return when (role) {
            com.web3community.user.dto.AuthRole.USER -> listOf("READ_PROFILE", "UPDATE_OWN_PROFILE")
            com.web3community.user.dto.AuthRole.MODERATOR -> listOf("READ_PROFILE", "UPDATE_OWN_PROFILE", "DELETE_OWN_POST", "MANAGE_USERS")
            com.web3community.user.dto.AuthRole.ADMIN -> listOf(
                "READ_PROFILE", "UPDATE_ANY_PROFILE", "DELETE_OWN_ACCOUNT", 
                "MANAGE_USERS", "MANAGE_CONTENT", "MANAGE_NOTIFICATIONS", "SYSTEM_ADMIN"
            )
        }
    }
}