package com.mindbridge.oye.dto

import com.mindbridge.oye.domain.Gender
import com.mindbridge.oye.domain.Role
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
