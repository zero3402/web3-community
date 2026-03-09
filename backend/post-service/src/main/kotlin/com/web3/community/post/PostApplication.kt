package com.web3.community.post

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(scanBasePackages = ["com.web3.community"])
class PostApplication

fun main(args: Array<String>) {
    runApplication<PostApplication>(*args)
}
