package com.mindbridge.oye.dto

import com.mindbridge.oye.domain.Compatibility
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.time.LocalDateTime

@Schema(description = "궁합 응답")
data class CompatibilityResponse(
    @Schema(description = "궁합 고유 ID", example = "1")
    val id: Long,

    @Schema(description = "궁합 점수 (0-100)", example = "85")
    val score: Int,

    @Schema(description = "AI가 생성한 궁합 본문", example = "오늘 두 분의 궁합은...")
    val content: String,

    @Schema(description = "관계별 특화 운세 (애정운/우정운/가족운/직장운)", example = "서로의 작은 표정 변화도 놓치지 않는 다정한 하루가 돼요.")
    val relationFortune: String? = null,

    @Schema(description = "궁합 대상 날짜 (YYYY-MM-DD)", example = "2025-06-15")
    val date: LocalDate,

    @Schema(description = "궁합 생성일시", example = "2025-06-15T08:00:00")
    val createdAt: LocalDateTime
) {
    companion object {
        fun from(compatibility: Compatibility): CompatibilityResponse {
            return CompatibilityResponse(
                id = compatibility.id!!,
                score = compatibility.score,
                content = compatibility.content,
                relationFortune = compatibility.relationFortune,
                date = compatibility.date,
                createdAt = compatibility.createdAt
            )
        }
    }
}
