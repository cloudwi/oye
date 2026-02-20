package com.mindbridge.oye.domain

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "혈액형", enumAsRef = true)
enum class BloodType {
    @Schema(description = "A형")
    A,

    @Schema(description = "B형")
    B,

    @Schema(description = "O형")
    O,

    @Schema(description = "AB형")
    AB
}
