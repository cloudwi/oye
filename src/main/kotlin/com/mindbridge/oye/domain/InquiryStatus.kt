package com.mindbridge.oye.domain

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "문의 상태", enumAsRef = true)
enum class InquiryStatus {
    @Schema(description = "답변 대기")
    PENDING,

    @Schema(description = "답변 완료")
    ANSWERED
}
