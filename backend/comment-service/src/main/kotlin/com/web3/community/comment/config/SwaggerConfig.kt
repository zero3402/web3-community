package com.web3.community.comment.config

import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.info.Info
import org.springframework.context.annotation.Configuration

@Configuration
@OpenAPIDefinition(
    info = Info(
        title = "Comment Service API",
        description = "댓글 관리 API (중첩 댓글, 실시간 스트리밍)",
        version = "1.0.0"
    )
)
class SwaggerConfig
