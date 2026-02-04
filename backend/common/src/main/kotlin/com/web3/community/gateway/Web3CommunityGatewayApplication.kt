package com.web3community.gateway

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.gateway.route.RouteLocator
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder
import org.springframework.context.annotation.Bean

@SpringBootApplication
class Web3CommunityGatewayApplication

@Bean
fun customRouteLocator(routeLocatorBuilder: RouteLocatorBuilder): RouteLocator {
    return routeLocatorBuilder.routes()
        // User Service - MVC (동기식)
        .route("user-service") { r ->
            r.path("/api/users/**", "/api/auth/**")
                .filters {
                    it.circuitBreaker { circuit -> circuit.setName("user-service-cb") }
                    it.retry(3)
                    it.stripPrefix(1)
                }
                .uri("http://localhost:8081")
        }
        
        // Post Service - WebFlux (리액티브)
        .route("post-service") { r ->
            r.path("/api/posts/**")
                .filters {
                    it.circuitBreaker { circuit -> circuit.setName("post-service-cb") }
                    it.retry(2)
                    it.stripPrefix(1)
                }
                .uri("http://localhost:8082")
        }
        
        // Notification Service - WebFlux (리액티브)
        .route("notification-service") { r ->
            r.path("/api/notifications/**")
                .filters {
                    it.circuitBreaker { circuit -> circuit.setName("notification-service-cb") }
                    it.retry(2)
                    it.stripPrefix(1)
                }
                .uri("http://localhost:8083")
        }
        
        // Auth Service - 새로 추가
        .route("auth-service") { r ->
            r.path("/api/security/**")
                .filters {
                    it.circuitBreaker { circuit -> circuit.setName("auth-service-cb") }
                    it.retry(3)
                    it.stripPrefix(1)
                }
                .uri("http://localhost:8084")
        }
        
        // Comment Service - 새로 추가
        .route("comment-service") { r ->
            r.path("/api/comments/**")
                .filters {
                    it.circuitBreaker { circuit -> circuit.setName("comment-service-cb") }
                    it.retry(2)
                    it.stripPrefix(1)
                }
                .uri("http://localhost:8085")
        }
        .build()
}

fun main(args: Array<String>) {
    runApplication<Web3CommunityGatewayApplication>(*args)
}