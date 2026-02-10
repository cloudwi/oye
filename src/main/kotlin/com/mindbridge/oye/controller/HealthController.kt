package com.mindbridge.oye.controller

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class HealthController {

    @GetMapping("/health")
    fun health(): Map<String, String> {
        return mapOf("status" to "ok")
    }

    @GetMapping("/favicon.ico")
    fun favicon(): ResponseEntity<Void> {
        return ResponseEntity.noContent().build()
    }
}
