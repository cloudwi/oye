package com.mindbridge.oye.util

import com.mindbridge.oye.domain.BloodType
import com.mindbridge.oye.domain.CalendarType
import com.mindbridge.oye.domain.Gender
import com.mindbridge.oye.domain.User

object UserProfileBuilder {

    fun buildProfileParts(user: User, nameLabel: String = "이름"): List<String> {
        val genderText = when (user.gender) {
            Gender.MALE -> "남성"
            Gender.FEMALE -> "여성"
            null -> "미지정"
        }
        val calendarText = when (user.calendarType) {
            CalendarType.SOLAR -> "양력"
            CalendarType.LUNAR -> "음력"
            null -> "양력"
        }
        val bloodTypeText = when (user.bloodType) {
            BloodType.A -> "A형"
            BloodType.B -> "B형"
            BloodType.O -> "O형"
            BloodType.AB -> "AB형"
            null -> null
        }

        val parts = mutableListOf<String>()
        user.name?.let { parts.add("$nameLabel: $it") }
        parts.add("$genderText, ${user.birthDate}생 ($calendarText)")
        user.birthTime?.let { parts.add("태어난 시각: $it") }
        user.occupation?.let { parts.add("직업: $it") }
        user.mbti?.let { parts.add("MBTI: $it") }
        bloodTypeText?.let { parts.add("혈액형: $it") }
        user.interests?.let { parts.add("관심사: $it") }
        return parts
    }
}
