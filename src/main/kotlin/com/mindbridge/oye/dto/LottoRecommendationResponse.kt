package com.mindbridge.oye.dto

import com.mindbridge.oye.domain.LottoRecommendation
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "로또 추천 번호 응답")
data class LottoRecommendationResponse(
    @Schema(description = "추천 고유 ID", example = "1")
    val id: Long,

    @Schema(description = "회차", example = "1130")
    val round: Int,

    @Schema(description = "세트 번호", example = "1")
    val setNumber: Int,

    @Schema(description = "추천 번호 6개", example = "[3, 11, 19, 25, 33, 42]")
    val numbers: List<Int>,

    @Schema(description = "등수 (미당첨 시 null)", example = "1등")
    val rank: String?,

    @Schema(description = "일치 개수", example = "3")
    val matchCount: Int,

    @Schema(description = "보너스 번호 일치 여부", example = "false")
    val bonusMatch: Boolean,

    @Schema(description = "당첨 평가 완료 여부", example = "true")
    val evaluated: Boolean,

    @Schema(description = "당첨 금액", example = "50000")
    val prizeAmount: Long?,

    @Schema(description = "생성일시", example = "2025-06-15T08:00:00")
    val createdAt: LocalDateTime
) {
    companion object {
        fun from(recommendation: LottoRecommendation): LottoRecommendationResponse {
            return LottoRecommendationResponse(
                id = recommendation.id!!,
                round = recommendation.round,
                setNumber = recommendation.setNumber,
                numbers = recommendation.numbers,
                rank = recommendation.rank?.description,
                matchCount = recommendation.matchCount,
                bonusMatch = recommendation.bonusMatch,
                evaluated = recommendation.evaluated,
                prizeAmount = recommendation.prizeAmount,
                createdAt = recommendation.createdAt
            )
        }
    }
}
