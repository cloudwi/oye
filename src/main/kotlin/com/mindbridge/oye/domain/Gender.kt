package com.mindbridge.oye.domain

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "성별", enumAsRef = true)
enum class Gender {
    @Schema(description = "남성")
    MALE,

    @Schema(description = "여성")
    FEMALE
}
