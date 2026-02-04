package com.web3community.post

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.openfeign.EnableFeignClients
import org.springframework.cache.annotation.EnableCaching

@SpringBootApplication(scanBasePackages = ["com.web3community"])
@EnableFeignClients
@EnableCaching
class PostServiceApplication

fun main(args: Array<String>) {
    runApplication<PostServiceApplication>(*args)
}