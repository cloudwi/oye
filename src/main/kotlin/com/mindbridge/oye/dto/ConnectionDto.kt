package com.mindbridge.oye.dto

import com.mindbridge.oye.domain.RelationType
import com.mindbridge.oye.domain.User
import com.mindbridge.oye.domain.UserConnection
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "연결 요청")
data class ConnectRequest(
    @Schema(description = "상대방 초대 코드", example = "A1B2C3", nullable = true)
    val code: String? = null,

    @Schema(description = "상대방 닉네임", example = "fortune_lover", nullable = true)
    val nickname: String? = null,

    @Schema(description = "관계 유형")
    val relationType: RelationType
)

@Schema(description = "연결 응답")
data class ConnectionResponse(
    @Schema(description = "연결 고유 ID", example = "1")
    val id: Long,

    @Schema(description = "상대방 유저 ID", example = "42")
    val partnerId: Long,

    @Schema(description = "상대방 이름", example = "홍길동", nullable = true)
    val partnerName: String?,

    @Schema(description = "상대방 닉네임", example = "fortune_lover", nullable = true)
    val partnerNickname: String?,

    @Schema(description = "관계 유형")
    val relationType: RelationType,

    @Schema(description = "최근 궁합 점수", example = "85")
    val latestScore: Int?,

    @Schema(description = "오늘의 궁합 한마디", example = "오늘은 서로 말이 잘 통하는 날이에요.")
    val latestContent: String?,

    @Schema(description = "연결 생성일시", example = "2025-06-15T08:00:00")
    val createdAt: LocalDateTime
) {
    companion object {
        fun from(connection: UserConnection, currentUser: User, latestScore: Int?, latestContent: String? = null): ConnectionResponse {
            val partner = if (connection.user.id == currentUser.id) connection.partner else connection.user
            return ConnectionResponse(
                id = connection.id!!,
                partnerId = partner.id!!,
                partnerName = partner.name,
                partnerNickname = partner.nickname,
                relationType = connection.relationType,
                latestScore = latestScore,
                latestContent = latestContent,
                createdAt = connection.createdAt
            )
        }
    }
}

@Schema(description = "내 초대 코드 응답")
data class MyCodeResponse(
    @Schema(description = "6자리 초대 코드", example = "A1B2C3")
    val code: String,

    @Schema(description = "닉네임", example = "fortune_lover", nullable = true)
    val nickname: String?
)
