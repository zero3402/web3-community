package com.web3community.gateway

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

/**
 * API Gateway 애플리케이션 진입점
 *
 * ## 역할
 * - 클라이언트(Vue.js)와 내부 마이크로서비스 사이의 단일 진입 창구(Single Entry Point)
 * - JWT 인증/인가, CORS, Rate Limiting, 로깅을 중앙에서 처리
 * - Eureka를 통한 서비스 디스커버리 및 로드밸런싱 (`lb://` 스킴)
 *
 * ## 기술 선택 근거
 * - **Spring Cloud Gateway**: Webflux(Netty) 기반 비동기 논블로킹 처리로 높은 처리량 확보.
 *   Zuul(서블릿/블로킹) 대비 동일 자원에서 더 많은 동시 연결 처리 가능.
 * - **Kotlin Coroutines**: 복잡한 필터 체인을 동기 코드처럼 읽기 쉽게 작성.
 *
 * ## 주의사항
 * - spring-boot-starter-web(Tomcat) 의존성과 함께 사용 불가.
 *   Gateway는 반드시 Netty 위에서 동작해야 한다.
 */
@SpringBootApplication
class ApiGatewayApplication

/**
 * 메인 함수
 *
 * Spring Boot 애플리케이션을 시작한다.
 * JVM 옵션 예시: -Xms256m -Xmx512m (Netty는 Tomcat보다 낮은 메모리로 운영 가능)
 */
fun main(args: Array<String>) {
    runApplication<ApiGatewayApplication>(*args)
}
