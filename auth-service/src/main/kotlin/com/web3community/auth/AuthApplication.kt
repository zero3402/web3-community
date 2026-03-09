package com.web3community.auth

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.openfeign.EnableFeignClients

/**
 * Auth Service 메인 애플리케이션 클래스
 *
 * 이 서비스는 Web3 커뮤니티 플랫폼의 인증/인가를 전담한다.
 *
 * 주요 기능:
 * - 일반 로그인/회원가입 (이메일 + 비밀번호)
 * - OAuth2 소셜 로그인 (Google / Naver / Kakao)
 * - JWT Access Token 발급 및 검증
 * - Refresh Token Rotation (Redis 기반)
 * - 회원가입 이벤트 Kafka 발행
 *
 * 포트: 8081
 * 기술 스택: Spring Boot MVC, Spring Security, Redis, OpenFeign, Kafka
 */
@SpringBootApplication
@EnableFeignClients(basePackages = ["com.web3community.auth.client"])
// OpenFeign 클라이언트를 활성화한다.
// basePackages를 명시하여 불필요한 스캔 범위를 줄이고 시작 성능을 개선한다.
class AuthApplication

/**
 * 애플리케이션 진입점
 *
 * @param args 커맨드라인 인수 (Spring Boot가 자동으로 처리)
 */
fun main(args: Array<String>) {
    runApplication<AuthApplication>(*args)
}
