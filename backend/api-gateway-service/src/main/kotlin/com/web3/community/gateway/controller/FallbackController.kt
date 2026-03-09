package com.web3.community.gateway.controller

import com.web3.community.common.dto.ApiResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/fallback")
class FallbackController {

    @GetMapping("/post")
    fun postFallback(): Mono<ApiResponse<Nothing>> {
        return Mono.just(ApiResponse.error("SERVICE_UNAVAILABLE", "Post service is temporarily unavailable"))
    }
}
