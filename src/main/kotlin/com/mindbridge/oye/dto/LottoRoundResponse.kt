package com.mindbridge.oye.dto

import com.mindbridge.oye.domain.LottoRound
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

@Schema(description = "로또 회차 당첨 번호 응답")
data class LottoRoundResponse(
    @Schema(description = "회차", example = "1130")
    val round: Int,

    @Schema(description = "당첨 번호 6개", example = "[3, 11, 19, 25, 33, 42]")
    val numbers: List<Int>,

    @Schema(description = "보너스 번호", example = "7")
    val bonusNumber: Int,

    @Schema(description = "추첨일", example = "2025-06-14")
    val drawDate: LocalDate,

    @Schema(description = "1등 당첨금액", example = "2000000000")
    val firstPrizeAmount: Long?
) {
    companion object {
        fun from(lottoRound: LottoRound): LottoRoundResponse {
            return LottoRoundResponse(
                round = lottoRound.round,
                numbers = lottoRound.numbers,
                bonusNumber = lottoRound.bonusNumber,
                drawDate = lottoRound.drawDate,
                firstPrizeAmount = lottoRound.firstPrizeAmount
            )
        }
    }
}
