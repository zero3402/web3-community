package com.web3.community.post.config

import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.info.Info
import org.springframework.context.annotation.Configuration

@Configuration
@OpenAPIDefinition(
    info = Info(
        title = "Post Service API",
        description = "게시글 및 카테고리 관리 API",
        version = "1.0.0"
    )
)
class SwaggerConfig
