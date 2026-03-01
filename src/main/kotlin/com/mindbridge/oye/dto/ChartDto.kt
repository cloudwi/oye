package com.mindbridge.oye.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

@Schema(description = "점수 추이 데이터 포인트")
data class ScoreTrendPoint(
    @Schema(description = "날짜", example = "2026-03-01")
    val date: LocalDate,

    @Schema(description = "점수", example = "75")
    val score: Int
)

@Schema(description = "기록 날짜 응답")
data class RecordDatesResponse(
    @Schema(description = "연월 (YYYY-MM)", example = "2026-03")
    val yearMonth: String,

    @Schema(description = "기록이 있는 날짜 목록")
    val dates: List<LocalDate>
)
