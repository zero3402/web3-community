package com.web3.community.user.config

import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.info.Info
import org.springframework.context.annotation.Configuration

@Configuration
@OpenAPIDefinition(
    info = Info(
        title = "User Service API",
        description = "사용자 프로필 관리 API (CRUD, 역할 관리)",
        version = "1.0.0"
    )
)
class SwaggerConfig
