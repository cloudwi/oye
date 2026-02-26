package com.mindbridge.oye.repository

import com.mindbridge.oye.domain.AppVersionConfig
import org.springframework.data.jpa.repository.JpaRepository

interface AppVersionConfigRepository : JpaRepository<AppVersionConfig, Long> {
    fun findByPlatform(platform: String): AppVersionConfig?
}
