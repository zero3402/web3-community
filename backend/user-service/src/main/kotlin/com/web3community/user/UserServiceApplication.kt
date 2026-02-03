package com.web3community.user

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.context.annotation.ComponentScan
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

/**
 * 사용자 서비스 메인 애플리케이션 클래스
 * Spring Boot 애플리케이션의 시작점
 * WebFlux 기반 리액티브 마이크로서비스
 */
@SpringBootApplication
@ComponentScan(basePackages = ["com.web3community.user", "com.web3community.util"])
@EntityScan(basePackages = ["com.web3community.user"])
@EnableJpaRepositories(basePackages = ["com.web3community.user"])
class UserServiceApplication

/**
 * 애플리케이션 메인 함수
 * Spring Boot 애플리케이션 시작
 */
fun main(args: Array<String>) {
    org.springframework.boot.runApplication<UserServiceApplication>(*args)
}