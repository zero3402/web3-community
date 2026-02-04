package com.web3community.user

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.context.annotation.ComponentScan
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

@SpringBootApplication
@ComponentScan(basePackages = ["com.web3community.user", "com.web3community.util"])
@EntityScan(basePackages = ["com.web3community.user"])
@EnableJpaRepositories(basePackages = ["com.web3community.user"])
class UserServiceApplication

fun main(args: Array<String>) {
    org.springframework.boot.runApplication<UserServiceApplication>(*args)
}