package com.mindbridge.oye.dto

import com.mindbridge.oye.domain.Fortune
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.time.LocalDateTime

@Schema(description = "예감 응답")
data class FortuneResponse(
    @Schema(description = "예감 고유 ID", example = "1")
    val id: Long,

    @Schema(description = "AI가 생성한 예감 본문", example = "오후에 반가운 연락이 온다.")
    val content: String,

    @Schema(description = "예감 대상 날짜 (YYYY-MM-DD)", example = "2025-06-15")
    val date: LocalDate,

    @Schema(description = "예감 점수 (1~100)", example = "75")
    val score: Int?,

    @Schema(description = "예감 생성일시", example = "2025-06-15T08:00:00")
    val createdAt: LocalDateTime
) {
    companion object {
        fun from(fortune: Fortune): FortuneResponse {
            return FortuneResponse(
                id = fortune.id!!,
                content = fortune.content,
                date = fortune.date,
                score = fortune.score,
                createdAt = fortune.createdAt
            )
        }
    }
}
