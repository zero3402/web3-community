package com.web3.community.user

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(scanBasePackages = ["com.web3.community"])
class UserApplication

fun main(args: Array<String>) {
    runApplication<UserApplication>(*args)
}
