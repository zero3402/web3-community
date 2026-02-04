package com.web3community.auth

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.openfeign.EnableFeignClients
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.transaction.annotation.EnableTransactionManagement

@SpringBootApplication(scanBasePackages = ["com.web3community"])
@EnableFeignClients
@EnableAsync
@EnableTransactionManagement
@EnableJpaRepositories(basePackages = ["com.web3community.auth.domain.repository"])
class AuthDDDServiceApplication

fun main(args: Array<String>) {
    runApplication<AuthDDDServiceApplication>(*args)
}