package com.web3community.comment

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.openfeign.EnableFeignClients
import org.springframework.cache.annotation.EnableCaching
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication(scanBasePackages = ["com.web3community"])
@EnableFeignClients
@EnableCaching
@EnableAsync
@EnableScheduling
class CommentServiceApplication

fun main(args: Array<String>) {
    runApplication<CommentServiceApplication>(*args)
}