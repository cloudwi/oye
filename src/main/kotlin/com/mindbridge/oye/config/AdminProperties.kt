package com.mindbridge.oye.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "oye")
data class AdminProperties(
    val adminUserIds: List<Long> = emptyList()
)
