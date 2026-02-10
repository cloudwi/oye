package com.mindbridge.oye.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.Comment
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "fortunes")
@Comment("AI 생성 예감")
class Fortune(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("예감 고유 ID")
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @Comment("예감 대상 사용자")
    val user: User,

    @Column(length = 1000, nullable = false)
    @Comment("AI가 생성한 예감 본문")
    val content: String,

    @Column(nullable = false)
    @Comment("예감 대상 날짜")
    val date: LocalDate,

    @Column(nullable = false, updatable = false)
    @Comment("예감 생성일시")
    val createdAt: LocalDateTime = LocalDateTime.now()
)
