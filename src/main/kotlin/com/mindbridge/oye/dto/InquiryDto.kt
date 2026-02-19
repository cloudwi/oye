package com.mindbridge.oye.dto

import com.mindbridge.oye.domain.Inquiry
import com.mindbridge.oye.domain.InquiryStatus
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

@Schema(description = "문의 작성 요청")
data class InquiryCreateRequest(
    @field:NotBlank(message = "제목은 필수입니다.")
    @field:Size(max = 100, message = "제목은 100자 이내여야 합니다.")
    @Schema(description = "문의 제목", example = "앱 사용 중 오류가 발생합니다", requiredMode = Schema.RequiredMode.REQUIRED)
    val title: String,

    @field:NotBlank(message = "내용은 필수입니다.")
    @field:Size(max = 2000, message = "내용은 2000자 이내여야 합니다.")
    @Schema(description = "문의 내용", example = "예감 조회 시 화면이 멈추는 현상이 있습니다.", requiredMode = Schema.RequiredMode.REQUIRED)
    val content: String
)

@Schema(description = "문의 답변 요청")
data class InquiryReplyRequest(
    @field:NotBlank(message = "답변 내용은 필수입니다.")
    @field:Size(max = 2000, message = "답변은 2000자 이내여야 합니다.")
    @Schema(description = "관리자 답변 내용", example = "확인 후 수정하겠습니다. 감사합니다.", requiredMode = Schema.RequiredMode.REQUIRED)
    val content: String
)

@Schema(description = "문의 응답")
data class InquiryResponse(
    @Schema(description = "문의 고유 ID", example = "1")
    val id: Long,

    @Schema(description = "문의 제목", example = "앱 사용 중 오류가 발생합니다")
    val title: String,

    @Schema(description = "문의 내용", example = "예감 조회 시 화면이 멈추는 현상이 있습니다.")
    val content: String,

    @Schema(description = "문의 상태", example = "PENDING")
    val status: InquiryStatus,

    @Schema(description = "관리자 답변", example = "확인 후 수정하겠습니다.", nullable = true)
    val adminReply: String?,

    @Schema(description = "관리자 답변 일시", nullable = true)
    val adminRepliedAt: LocalDateTime?,

    @Schema(description = "문의 작성일시")
    val createdAt: LocalDateTime
) {
    companion object {
        fun from(inquiry: Inquiry): InquiryResponse {
            return InquiryResponse(
                id = inquiry.id!!,
                title = inquiry.title,
                content = inquiry.content,
                status = inquiry.status,
                adminReply = inquiry.adminReply,
                adminRepliedAt = inquiry.adminRepliedAt,
                createdAt = inquiry.createdAt
            )
        }
    }
}
