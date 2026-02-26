package com.mindbridge.oye.controller

import com.mindbridge.oye.dto.AppUpdateCheckResponse
import com.mindbridge.oye.repository.AppVersionConfigRepository
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/app")
class AppController(
    private val appVersionConfigRepository: AppVersionConfigRepository
) {

    @GetMapping("/check-update")
    fun checkUpdate(
        @RequestParam platform: String,
        @RequestParam version: String
    ): ResponseEntity<AppUpdateCheckResponse> {
        val config = appVersionConfigRepository.findByPlatform(platform.lowercase())
            ?: return ResponseEntity.notFound().build()

        val forceUpdate = compareVersions(version, config.minVersion) < 0

        return ResponseEntity.ok(
            AppUpdateCheckResponse(
                forceUpdate = forceUpdate,
                minVersion = config.minVersion,
                storeUrl = config.storeUrl
            )
        )
    }

    private fun compareVersions(current: String, minimum: String): Int {
        val currentParts = current.split(".").map { it.toIntOrNull() ?: 0 }
        val minimumParts = minimum.split(".").map { it.toIntOrNull() ?: 0 }
        val maxLength = maxOf(currentParts.size, minimumParts.size)

        for (i in 0 until maxLength) {
            val c = currentParts.getOrElse(i) { 0 }
            val m = minimumParts.getOrElse(i) { 0 }
            if (c != m) return c.compareTo(m)
        }
        return 0
    }
}
