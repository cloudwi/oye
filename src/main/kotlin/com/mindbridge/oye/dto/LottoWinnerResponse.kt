package com.mindbridge.oye.dto

import com.mindbridge.oye.domain.LottoRecommendation
import com.mindbridge.oye.domain.LottoRound
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

@Schema(description = "로또 당첨자 게시판 응답 (익명)")
data class LottoWinnerResponse(
    @Schema(description = "회차", example = "1130")
    val round: Int,

    @Schema(description = "등수", example = "3등")
    val rank: String,

    @Schema(description = "추천 번호 6개", example = "[3, 11, 19, 25, 33, 42]")
    val numbers: List<Int>,

    @Schema(description = "일치 개수", example = "5")
    val matchCount: Int,

    @Schema(description = "보너스 번호 일치 여부", example = "false")
    val bonusMatch: Boolean,

    @Schema(description = "추첨일", example = "2025-06-14")
    val drawDate: LocalDate?
) {
    companion object {
        fun from(recommendation: LottoRecommendation): LottoWinnerResponse {
            return LottoWinnerResponse(
                round = recommendation.round,
                rank = recommendation.rank!!.description,
                numbers = recommendation.numbers,
                matchCount = recommendation.matchCount,
                bonusMatch = recommendation.bonusMatch,
                drawDate = null
            )
        }

        fun from(recommendation: LottoRecommendation, lottoRound: LottoRound?): LottoWinnerResponse {
            return LottoWinnerResponse(
                round = recommendation.round,
                rank = recommendation.rank!!.description,
                numbers = recommendation.numbers,
                matchCount = recommendation.matchCount,
                bonusMatch = recommendation.bonusMatch,
                drawDate = lottoRound?.drawDate
            )
        }
    }
}
