package com.mindbridge.oye.dto

import com.mindbridge.oye.domain.Fortune
import java.time.LocalDate
import java.time.LocalDateTime

data class FortuneResponse(
    val id: Long,
    val content: String,
    val date: LocalDate,
    val createdAt: LocalDateTime
) {
    companion object {
        fun from(fortune: Fortune): FortuneResponse {
            return FortuneResponse(
                id = fortune.id!!,
                content = fortune.content,
                date = fortune.date,
                createdAt = fortune.createdAt
            )
        }
    }
}
