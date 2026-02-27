package com.mindbridge.oye.dto

import com.mindbridge.oye.domain.PushNotification
import com.mindbridge.oye.domain.TargetType
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import java.time.LocalDateTime

@Schema(description = "푸시 토큰 등록/해제 요청")
data class PushTokenRequest(
    @Schema(description = "Expo 푸시 토큰 (null이면 토큰 해제)", example = "ExponentPushToken[xxxxxxxxxxxxxxxxxxxxxx]", nullable = true)
    val token: String? = null
)

@Schema(description = "푸시 알림 발송 요청")
data class SendPushRequest(
    @field:NotBlank(message = "제목은 필수입니다.")
    @Schema(description = "알림 제목", example = "공지사항", requiredMode = Schema.RequiredMode.REQUIRED)
    val title: String,

    @field:NotBlank(message = "내용은 필수입니다.")
    @Schema(description = "알림 내용", example = "새로운 기능이 추가되었습니다!", requiredMode = Schema.RequiredMode.REQUIRED)
    val body: String,

    @Schema(description = "발송 대상 유형 (ALL: 전체, SPECIFIC: 특정 사용자)", example = "ALL", requiredMode = Schema.RequiredMode.REQUIRED)
    val targetType: TargetType,

    @Schema(description = "대상 사용자 ID 목록 (targetType이 SPECIFIC일 때 필수)", nullable = true)
    val targetUserIds: List<Long>? = null
)

@Schema(description = "푸시 알림 발송 결과 응답")
data class PushNotificationResponse(
    @Schema(description = "푸시 알림 ID", example = "1")
    val id: Long,

    @Schema(description = "알림 제목", example = "공지사항")
    val title: String,

    @Schema(description = "알림 내용", example = "새로운 기능이 추가되었습니다!")
    val body: String,

    @Schema(description = "발송 대상 유형", example = "ALL")
    val targetType: TargetType,

    @Schema(description = "대상 사용자 ID 목록", nullable = true)
    val targetUserIds: List<Long>?,

    @Schema(description = "발송 성공 수", example = "95")
    val sentCount: Int,

    @Schema(description = "발송 실패 수", example = "5")
    val failCount: Int,

    @Schema(description = "발송한 관리자 ID", example = "1")
    val sentBy: Long,

    @Schema(description = "생성일시")
    val createdAt: LocalDateTime
) {
    companion object {
        fun from(notification: PushNotification): PushNotificationResponse {
            return PushNotificationResponse(
                id = notification.id!!,
                title = notification.title,
                body = notification.body,
                targetType = notification.targetType,
                targetUserIds = notification.targetUserIds?.split(",")?.map { it.trim().toLong() },
                sentCount = notification.sentCount,
                failCount = notification.failCount,
                sentBy = notification.sentBy,
                createdAt = notification.createdAt
            )
        }
    }
}
