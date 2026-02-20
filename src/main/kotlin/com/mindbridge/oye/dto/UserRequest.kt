package com.mindbridge.oye.dto

import com.mindbridge.oye.domain.BloodType
import com.mindbridge.oye.domain.CalendarType
import com.mindbridge.oye.domain.Gender
import com.mindbridge.oye.domain.SocialProvider
import com.mindbridge.oye.domain.User
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Past
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@Schema(description = "사용자 정보 수정 요청")
data class UserUpdateRequest(
    @field:NotBlank(message = "이름은 필수입니다.")
    @Schema(description = "사용자 이름", example = "홍길동", requiredMode = Schema.RequiredMode.REQUIRED)
    val name: String,

    @field:Past(message = "생년월일은 과거 날짜여야 합니다.")
    @Schema(description = "생년월일 (YYYY-MM-DD)", example = "1990-01-15", requiredMode = Schema.RequiredMode.REQUIRED)
    val birthDate: LocalDate,

    @Schema(description = "태어난 시각 (HH:mm)", example = "14:30", nullable = true)
    val birthTime: LocalTime? = null,

    @Schema(description = "성별 (null이면 기존 값 유지)", example = "MALE", nullable = true)
    val gender: Gender? = null,

    @Schema(description = "달력 유형 (null이면 기존 값 유지)", example = "SOLAR", nullable = true)
    val calendarType: CalendarType? = null,

    @field:Size(max = 50, message = "직업은 50자 이내여야 합니다.")
    @Schema(description = "직업", example = "개발자", nullable = true)
    val occupation: String? = null,

    @field:Pattern(regexp = "^(E|I)(S|N)(T|F)(J|P)$", message = "올바른 MBTI 형식이 아닙니다.")
    @Schema(description = "MBTI (4자리)", example = "INFP", nullable = true)
    val mbti: String? = null,

    @Schema(description = "혈액형", example = "A", nullable = true)
    val bloodType: BloodType? = null,

    @field:Size(max = 100, message = "관심사는 100자 이내여야 합니다.")
    @Schema(description = "관심사/취미", example = "독서, 요리", nullable = true)
    val interests: String? = null
)

@Schema(description = "사용자 정보 응답")
data class UserResponse(
    @Schema(description = "사용자 고유 ID", example = "1")
    val id: Long,

    @Schema(description = "소셜 로그인 제공자 (KAKAO, APPLE)", example = "KAKAO", nullable = true)
    val provider: SocialProvider?,

    @Schema(description = "사용자 이름", example = "홍길동")
    val name: String,

    @Schema(description = "생년월일 (YYYY-MM-DD)", example = "1990-01-15")
    val birthDate: LocalDate,

    @Schema(description = "태어난 시각 (HH:mm)", example = "14:30", nullable = true)
    val birthTime: LocalTime?,

    @Schema(description = "성별", example = "MALE", nullable = true)
    val gender: Gender?,

    @Schema(description = "달력 유형", example = "SOLAR", nullable = true)
    val calendarType: CalendarType?,

    @Schema(description = "직업", example = "개발자", nullable = true)
    val occupation: String?,

    @Schema(description = "MBTI", example = "INFP", nullable = true)
    val mbti: String?,

    @Schema(description = "혈액형", example = "A", nullable = true)
    val bloodType: BloodType?,

    @Schema(description = "관심사/취미", example = "독서, 요리", nullable = true)
    val interests: String?,

    @Schema(description = "가입일시", example = "2025-01-01T00:00:00")
    val createdAt: LocalDateTime
) {
    companion object {
        fun from(user: User, provider: SocialProvider? = null): UserResponse {
            return UserResponse(
                id = user.id!!,
                provider = provider,
                name = user.name,
                birthDate = user.birthDate,
                birthTime = user.birthTime,
                gender = user.gender,
                calendarType = user.calendarType,
                occupation = user.occupation,
                mbti = user.mbti,
                bloodType = user.bloodType,
                interests = user.interests,
                createdAt = user.createdAt
            )
        }
    }
}
