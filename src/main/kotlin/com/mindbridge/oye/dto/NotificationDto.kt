package com.mindbridge.oye.dto

import com.mindbridge.oye.domain.UserNotification
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "사용자 알림 응답")
data class UserNotificationResponse(
    @Schema(description = "알림 고유 ID", example = "1")
    val id: Long,

    @Schema(description = "알림 제목", example = "오늘의 운세가 도착했어요!")
    val title: String,

    @Schema(description = "알림 내용", example = "오늘 하루도 좋은 일이 가득할 거예요.")
    val body: String,

    @Schema(description = "알림 유형", example = "FORTUNE")
    val type: String,

    @Schema(description = "읽음 여부", example = "false")
    val isRead: Boolean,

    @Schema(description = "추가 메타데이터 (JSON)", example = "{\"fortuneId\": 123}", nullable = true)
    val metadata: String?,

    @Schema(description = "생성일시", example = "2026-03-08T09:00:00")
    val createdAt: String
) {
    companion object {
        fun from(n: UserNotification) = UserNotificationResponse(
            id = n.id!!,
            title = n.title,
            body = n.body,
            type = n.type.name,
            isRead = n.isRead,
            metadata = n.metadata,
            createdAt = n.createdAt.toString()
        )
    }
}

@Schema(description = "읽지 않은 알림 수 응답")
data class UnreadCountResponse(
    @Schema(description = "읽지 않은 알림 수", example = "3")
    val count: Long
)
