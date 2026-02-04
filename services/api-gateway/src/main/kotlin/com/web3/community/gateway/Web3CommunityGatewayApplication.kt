package com.web3.community.gateway

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.cloud.gateway.route.RouteLocator
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder
import org.springframework.context.annotation.Bean
import org.springframework.boot.runApplication

@SpringBootApplication
class Web3CommunityGatewayApplication

@Bean
fun customRouteLocator(routeLocatorBuilder: RouteLocatorBuilder): RouteLocator {
    return routeLocatorBuilder.routes()
        // User Service - MVC (Synchronous)
        .route("user-service") { r ->
            r.path("/api/auth/**", "/api/users/**")
                .filters {
                    it.circuitBreaker { circuit -> circuit.setName("user-service-cb") }
                    it.retry(3)
                }
                .uri("http://localhost:8081")
        }
        
        // Post Service - WebFlux (Reactive)
        .route("post-service") { r ->
            r.path("/api/posts/**")
                .filters {
                    it.circuitBreaker { circuit -> circuit.setName("post-service-cb") }
                    it.retry(2)
                }
                .uri("http://localhost:8082")
        }
        
        // Notification Service - WebFlux (Reactive with SSE)
        .route("notification-service") { r ->
            r.path("/api/notifications/**")
                .filters {
                    it.circuitBreaker { circuit -> circuit.setName("notification-service-cb") }
                    it.retry(2)
                }
                .uri("http://localhost:8083")
        }
        .build()
}

fun main(args: Array<String>) {
    runApplication<Web3CommunityGatewayApplication>(*args)
}