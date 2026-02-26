package com.mindbridge.oye.dto

import com.mindbridge.oye.domain.Inquiry
import com.mindbridge.oye.domain.InquiryComment
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

@Schema(description = "문의 댓글 작성 요청")
data class InquiryCommentCreateRequest(
    @field:NotBlank(message = "댓글 내용은 필수입니다.")
    @field:Size(max = 2000, message = "댓글은 2000자 이내여야 합니다.")
    @Schema(description = "관리자 댓글 내용", example = "확인 후 수정하겠습니다. 감사합니다.", requiredMode = Schema.RequiredMode.REQUIRED)
    val content: String
)

@Schema(description = "문의 댓글 응답")
data class InquiryCommentResponse(
    @Schema(description = "댓글 고유 ID", example = "1")
    val id: Long,

    @Schema(description = "관리자 이름", example = "관리자", nullable = true)
    val adminName: String?,

    @Schema(description = "댓글 내용", example = "확인 후 수정하겠습니다.")
    val content: String,

    @Schema(description = "작성일시")
    val createdAt: LocalDateTime
) {
    companion object {
        fun from(comment: InquiryComment): InquiryCommentResponse {
            return InquiryCommentResponse(
                id = comment.id!!,
                adminName = comment.admin.name,
                content = comment.content,
                createdAt = comment.createdAt
            )
        }
    }
}

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

    @Schema(description = "관리자 댓글 목록")
    val comments: List<InquiryCommentResponse>,

    @Schema(description = "문의 작성일시")
    val createdAt: LocalDateTime
) {
    companion object {
        fun from(inquiry: Inquiry, comments: List<InquiryComment> = emptyList()): InquiryResponse {
            return InquiryResponse(
                id = inquiry.id!!,
                title = inquiry.title,
                content = inquiry.content,
                status = inquiry.status,
                comments = comments.map { InquiryCommentResponse.from(it) },
                createdAt = inquiry.createdAt
            )
        }
    }
}
