package com.mindbridge.oye.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "내 로또 통계")
data class LottoMyStatsResponse(
    @Schema(description = "누적 당첨 금액", example = "50000")
    val totalPrize: Long,
    @Schema(description = "당첨 횟수 (세트 기준)", example = "3")
    val winCount: Int
)
