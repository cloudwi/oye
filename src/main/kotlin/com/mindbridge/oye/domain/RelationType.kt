package com.mindbridge.oye.domain

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "관계 유형", enumAsRef = true)
enum class RelationType {
    @Schema(description = "연인")
    LOVER,

    @Schema(description = "친구")
    FRIEND,

    @Schema(description = "가족")
    FAMILY,

    @Schema(description = "동료")
    COLLEAGUE
}
