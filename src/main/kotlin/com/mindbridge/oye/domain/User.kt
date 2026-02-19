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

@Entity
@Table(name = "users")
@Comment("사용자 정보")
class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("사용자 고유 ID")
    val id: Long? = null,

    @Column(nullable = false)
    @Comment("사용자 이름")
    var name: String,

    @Column(nullable = false)
    @Comment("생년월일")
    var birthDate: LocalDate,

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    @Comment("성별 (MALE: 남성, FEMALE: 여성)")
    var gender: Gender? = null,

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    @Comment("달력 유형 (SOLAR: 양력, LUNAR: 음력)")
    var calendarType: CalendarType? = null,

    @Column(nullable = false, updatable = false)
    @Comment("가입일시")
    val createdAt: LocalDateTime = LocalDateTime.now()
)
