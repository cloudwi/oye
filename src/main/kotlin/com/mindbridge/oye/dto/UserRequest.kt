package com.mindbridge.oye.dto

import com.mindbridge.oye.domain.User
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Past
import java.time.LocalDate
import java.time.LocalDateTime

data class UserUpdateRequest(
    @field:NotBlank(message = "이름은 필수입니다.")
    val name: String,
    @field:Past(message = "생년월일은 과거 날짜여야 합니다.")
    val birthDate: LocalDate
)

data class UserResponse(
    val id: Long,
    val kakaoId: String,
    val name: String,
    val birthDate: LocalDate,
    val createdAt: LocalDateTime
) {
    companion object {
        fun from(user: User): UserResponse {
            return UserResponse(
                id = user.id!!,
                kakaoId = user.kakaoId,
                name = user.name,
                birthDate = user.birthDate,
                createdAt = user.createdAt
            )
        }
    }
}
