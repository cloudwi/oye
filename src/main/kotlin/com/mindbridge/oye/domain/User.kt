package com.mindbridge.oye.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.Comment
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@Entity
@Table(name = "users")
@Comment("사용자 정보")
class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("사용자 고유 ID")
    val id: Long? = null,

    @Comment("사용자 이름")
    var name: String? = null,

    @Column(nullable = false)
    @Comment("생년월일")
    var birthDate: LocalDate,

    @Comment("태어난 시각 (HH:mm)")
    var birthTime: LocalTime? = null,

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    @Comment("성별 (MALE: 남성, FEMALE: 여성)")
    var gender: Gender? = null,

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    @Comment("달력 유형 (SOLAR: 양력, LUNAR: 음력)")
    var calendarType: CalendarType? = null,

    @Column(length = 50)
    @Comment("직업")
    var occupation: String? = null,

    @Column(length = 4)
    @Comment("MBTI (예: INFP)")
    var mbti: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(length = 2)
    @Comment("혈액형 (A, B, O, AB)")
    var bloodType: BloodType? = null,

    @Column(length = 100)
    @Comment("관심사/취미")
    var interests: String? = null,

    @Column(unique = true, length = 6)
    @Comment("친구 초대 코드 (6자리 영숫자)")
    var connectCode: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @Comment("사용자 권한 (USER, ADMIN)")
    var role: Role = Role.USER,

    @Column(nullable = false, updatable = false)
    @Comment("가입일시")
    val createdAt: LocalDateTime = LocalDateTime.now()
)
