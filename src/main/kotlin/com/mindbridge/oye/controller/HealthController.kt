package com.mindbridge.oye.controller

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import javax.sql.DataSource

@RestController
class HealthController(
    private val dataSource: DataSource
) {

    @GetMapping("/health")
    fun health(): Map<String, Any> {
        val dbStatus = try {
            dataSource.connection.use { it.isValid(2) }
            "up"
        } catch (_: Exception) {
            "down"
        }
        return mapOf(
            "status" to if (dbStatus == "up") "ok" else "degraded",
            "db" to dbStatus
        )
    }

    @GetMapping("/favicon.ico")
    fun favicon(): ResponseEntity<Void> {
        return ResponseEntity.noContent().build()
    }
}
