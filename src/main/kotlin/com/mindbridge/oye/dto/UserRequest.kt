package com.mindbridge.oye.dto

import com.mindbridge.oye.domain.CalendarType
import com.mindbridge.oye.domain.Gender
import com.mindbridge.oye.domain.User
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Past
import java.time.LocalDate
import java.time.LocalDateTime

@Schema(description = "사용자 정보 수정 요청")
data class UserUpdateRequest(
    @field:NotBlank(message = "이름은 필수입니다.")
    @Schema(description = "사용자 이름", example = "홍길동", requiredMode = Schema.RequiredMode.REQUIRED)
    val name: String,

    @field:Past(message = "생년월일은 과거 날짜여야 합니다.")
    @Schema(description = "생년월일 (YYYY-MM-DD)", example = "1990-01-15", requiredMode = Schema.RequiredMode.REQUIRED)
    val birthDate: LocalDate,

    @Schema(description = "성별 (null이면 기존 값 유지)", example = "MALE", nullable = true)
    val gender: Gender? = null,

    @Schema(description = "달력 유형 (null이면 기존 값 유지)", example = "SOLAR", nullable = true)
    val calendarType: CalendarType? = null
)

@Schema(description = "사용자 정보 응답")
data class UserResponse(
    @Schema(description = "사용자 고유 ID", example = "1")
    val id: Long,

    @Schema(description = "카카오 OAuth2 고유 ID", example = "1234567890", nullable = true)
    val kakaoId: String?,

    @Schema(description = "사용자 이름", example = "홍길동")
    val name: String,

    @Schema(description = "생년월일 (YYYY-MM-DD)", example = "1990-01-15")
    val birthDate: LocalDate,

    @Schema(description = "성별", example = "MALE", nullable = true)
    val gender: Gender?,

    @Schema(description = "달력 유형", example = "SOLAR", nullable = true)
    val calendarType: CalendarType?,

    @Schema(description = "가입일시", example = "2025-01-01T00:00:00")
    val createdAt: LocalDateTime
) {
    companion object {
        fun from(user: User): UserResponse {
            return UserResponse(
                id = user.id!!,
                kakaoId = user.kakaoId,
                name = user.name,
                birthDate = user.birthDate,
                gender = user.gender,
                calendarType = user.calendarType,
                createdAt = user.createdAt
            )
        }
    }
}
