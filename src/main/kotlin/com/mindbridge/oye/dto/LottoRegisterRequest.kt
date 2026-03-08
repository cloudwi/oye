package com.mindbridge.oye.dto

import com.mindbridge.oye.domain.LottoSource
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "로또 번호 등록 요청")
data class LottoRegisterRequest(
    @Schema(description = "회차 번호", example = "1130")
    val round: Int,

    @Schema(description = "등록 출처 (MANUAL 또는 QR_SCAN)", example = "MANUAL")
    val source: LottoSource,

    @Schema(description = "번호 세트 목록 (각 6개 번호)", example = "[[1,2,3,4,5,6]]")
    val numberSets: List<List<Int>>
)
