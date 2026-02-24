package com.mindbridge.oye

import com.mindbridge.oye.config.AdminProperties
import com.mindbridge.oye.config.JwtProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(JwtProperties::class, AdminProperties::class)
class OyeApplication

fun main(args: Array<String>) {
    runApplication<OyeApplication>(*args)
}
