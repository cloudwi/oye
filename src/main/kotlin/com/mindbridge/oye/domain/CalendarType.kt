package com.mindbridge.oye.domain

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "달력 유형 (양력/음력)", enumAsRef = true)
enum class CalendarType {
    @Schema(description = "양력")
    SOLAR,

    @Schema(description = "음력")
    LUNAR
}
