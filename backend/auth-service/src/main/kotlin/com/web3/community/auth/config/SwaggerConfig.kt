package com.web3.community.auth.config

import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.info.Info
import org.springframework.context.annotation.Configuration

@Configuration
@OpenAPIDefinition(
    info = Info(
        title = "Auth Service API",
        description = "인증 및 권한 관리 API (회원가입, 로그인, 토큰 관리)",
        version = "1.0.0"
    )
)
class SwaggerConfig
