package com.mindbridge.oye.dto

import com.mindbridge.oye.domain.Gender
import com.mindbridge.oye.domain.Role
import com.mindbridge.oye.domain.SocialProvider
import com.mindbridge.oye.domain.User
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.time.LocalDateTime

@Schema(description = "관리자 로그인 요청")
data class AdminLoginRequest(
    @Schema(description = "리프레시 토큰", example = "eyJhbGciOiJIUzI1NiJ9...", requiredMode = Schema.RequiredMode.REQUIRED)
    val refreshToken: String
)

@Schema(description = "관리자 대시보드 통계")
data class AdminDashboardStats(
    @Schema(description = "전체 사용자 수", example = "150")
    val totalUsers: Long,

    @Schema(description = "전체 문의 수", example = "30")
    val totalInquiries: Long,

    @Schema(description = "미답변 문의 수", example = "5")
    val pendingInquiries: Long
)

@Schema(description = "관리자용 사용자 응답")
data class AdminUserResponse(
    @Schema(description = "사용자 고유 ID", example = "1")
    val id: Long,

    @Schema(description = "사용자 이름", example = "홍길동", nullable = true)
    val name: String?,

    @Schema(description = "생년월일", example = "1995-03-15")
    val birthDate: LocalDate,

    @Schema(description = "성별", example = "MALE")
    val gender: Gender?,

    @Schema(description = "권한", example = "USER")
    val role: Role,

    @Schema(description = "가입일시")
    val createdAt: LocalDateTime
) {
    companion object {
        fun from(user: User): AdminUserResponse {
            return AdminUserResponse(
                id = user.id!!,
                name = user.name,
                birthDate = user.birthDate,
                gender = user.gender,
                role = user.role,
                createdAt = user.createdAt
            )
        }
    }
}

@Schema(description = "관리자 카카오 인가코드 로그인 요청")
data class AdminKakaoCodeRequest(
    @Schema(description = "카카오 인가코드", requiredMode = Schema.RequiredMode.REQUIRED)
    val code: String,

    @Schema(description = "인가코드 발급 시 사용한 redirect_uri", requiredMode = Schema.RequiredMode.REQUIRED)
    val redirectUri: String
)

@Schema(description = "권한 변경 요청")
data class RoleUpdateRequest(
    @Schema(description = "변경할 권한", example = "ADMIN", requiredMode = Schema.RequiredMode.REQUIRED)
    val role: Role
)

@Schema(description = "관리자용 사용자 상세 응답")
data class AdminUserDetailResponse(
    @Schema(description = "사용자 고유 ID") val id: Long,
    @Schema(description = "사용자 이름") val name: String?,
    @Schema(description = "생년월일") val birthDate: LocalDate,
    @Schema(description = "성별") val gender: Gender?,
    @Schema(description = "소셜 로그인 제공자") val provider: SocialProvider?,
    @Schema(description = "권한") val role: Role,
    @Schema(description = "마지막 로그인 시각") val lastLoginAt: LocalDateTime?,
    @Schema(description = "가입일시") val createdAt: LocalDateTime
)

@Schema(description = "로그인 이력 응답")
data class LoginHistoryResponse(
    @Schema(description = "소셜 로그인 제공자") val provider: SocialProvider,
    @Schema(description = "IP 주소") val ipAddress: String?,
    @Schema(description = "User-Agent") val userAgent: String?,
    @Schema(description = "로그인 시각") val createdAt: LocalDateTime
)

@Schema(description = "관리자용 예감 응답")
data class AdminFortuneResponse(
    @Schema(description = "예감 날짜") val date: LocalDate,
    @Schema(description = "예감 점수") val score: Int?,
    @Schema(description = "예감 내용") val content: String
)

@Schema(description = "관리자용 궁합 응답")
data class AdminCompatibilityResponse(
    @Schema(description = "상대방 이름") val partnerName: String?,
    @Schema(description = "관계 유형") val relationType: com.mindbridge.oye.domain.RelationType,
    @Schema(description = "궁합 날짜") val date: LocalDate,
    @Schema(description = "궁합 점수") val score: Int,
    @Schema(description = "궁합 내용") val content: String
)

@Schema(description = "관리자용 로또 추천 응답")
data class AdminLottoResponse(
    @Schema(description = "회차") val round: Int,
    @Schema(description = "세트 번호") val setNumber: Int,
    @Schema(description = "추천 번호") val numbers: List<Int>,
    @Schema(description = "당첨 등수") val rank: String?,
    @Schema(description = "당첨 금액") val prizeAmount: Long?,
    @Schema(description = "평가 완료 여부") val evaluated: Boolean
)

@Schema(description = "관리자용 연결 응답")
data class AdminConnectionResponse(
    @Schema(description = "상대방 이름") val partnerName: String?,
    @Schema(description = "상대방 ID") val partnerId: Long,
    @Schema(description = "관계 유형") val relationType: com.mindbridge.oye.domain.RelationType
)

@Schema(description = "관리자용 그룹 응답")
data class AdminGroupResponse(
    @Schema(description = "그룹 이름") val name: String,
    @Schema(description = "멤버 수") val memberCount: Long,
    @Schema(description = "방장 여부") val isOwner: Boolean
)

