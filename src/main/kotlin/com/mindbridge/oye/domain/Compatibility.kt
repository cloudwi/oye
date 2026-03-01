package com.mindbridge.oye.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.hibernate.annotations.Comment
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(
    name = "compatibilities",
    uniqueConstraints = [UniqueConstraint(columnNames = ["connection_id", "date"])],
    indexes = [Index(name = "idx_compatibility_connection_date", columnList = "connection_id, date")]
)
@Comment("AI 생성 궁합 결과")
class Compatibility(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("궁합 고유 ID")
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @Comment("연결 정보")
    val connection: UserConnection,

    @Column(nullable = false)
    @Comment("궁합 점수 (0-100)")
    val score: Int,

    @Column(length = 500, nullable = false)
    @Comment("AI가 생성한 궁합 본문")
    val content: String,

    @Column(nullable = false)
    @Comment("궁합 대상 날짜")
    val date: LocalDate,

    @Column(nullable = false, updatable = false)
    @Comment("궁합 생성일시")
    val createdAt: LocalDateTime = LocalDateTime.now()
)
