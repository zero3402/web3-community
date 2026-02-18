package com.web3.community.gateway.filter

import com.web3.community.common.constants.AppConstants
import com.web3.community.common.jwt.JwtTokenProvider
import org.springframework.cloud.gateway.filter.GatewayFilter
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component

@Component
class JwtAuthGatewayFilterFactory(
    private val jwtTokenProvider: JwtTokenProvider
) : AbstractGatewayFilterFactory<JwtAuthGatewayFilterFactory.Config>(Config::class.java) {

    class Config

    override fun apply(config: Config): GatewayFilter {
        return GatewayFilter { exchange, chain ->
            val request = exchange.request
            val authHeader = request.headers.getFirst(AppConstants.AUTHORIZATION_HEADER)

            if (authHeader == null || !authHeader.startsWith(AppConstants.BEARER_PREFIX)) {
                exchange.response.statusCode = HttpStatus.UNAUTHORIZED
                return@GatewayFilter exchange.response.setComplete()
            }

            val token = authHeader.substring(AppConstants.BEARER_PREFIX.length)

            if (!jwtTokenProvider.validateToken(token)) {
                exchange.response.statusCode = HttpStatus.UNAUTHORIZED
                return@GatewayFilter exchange.response.setComplete()
            }

            val userId = jwtTokenProvider.getUserIdFromToken(token)
            val email = jwtTokenProvider.getEmailFromToken(token)
            val role = jwtTokenProvider.getRoleFromToken(token)
            val nickname = jwtTokenProvider.getNicknameFromToken(token)

            val modifiedRequest = request.mutate()
                .header(AppConstants.USER_ID_HEADER, userId.toString())
                .header(AppConstants.USER_EMAIL_HEADER, email)
                .header(AppConstants.USER_ROLE_HEADER, role)
                .header(AppConstants.USER_NICKNAME_HEADER, nickname)
                .build()

            chain.filter(exchange.mutate().request(modifiedRequest).build())
        }
    }

    override fun name(): String = "JwtAuthFilter"
}
