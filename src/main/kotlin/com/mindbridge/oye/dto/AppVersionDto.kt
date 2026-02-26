package com.mindbridge.oye.dto

import com.mindbridge.oye.domain.AppVersionConfig
import java.time.LocalDateTime

data class AppUpdateCheckResponse(
    val forceUpdate: Boolean,
    val minVersion: String,
    val storeUrl: String
)

data class AppVersionConfigResponse(
    val id: Long,
    val platform: String,
    val minVersion: String,
    val storeUrl: String,
    val updatedAt: LocalDateTime
) {
    companion object {
        fun from(config: AppVersionConfig) = AppVersionConfigResponse(
            id = config.id!!,
            platform = config.platform,
            minVersion = config.minVersion,
            storeUrl = config.storeUrl,
            updatedAt = config.updatedAt
        )
    }
}

data class AppVersionUpdateRequest(
    val minVersion: String,
    val storeUrl: String
)
