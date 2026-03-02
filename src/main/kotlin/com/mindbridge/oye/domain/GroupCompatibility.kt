package com.mindbridge.oye.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.hibernate.annotations.Comment
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(
    name = "group_compatibilities",
    uniqueConstraints = [UniqueConstraint(columnNames = ["group_id", "user_a_id", "user_b_id", "date"])],
    indexes = [Index(name = "idx_group_compatibility_group_date", columnList = "group_id, date")]
)
@Comment("그룹 내 멤버 간 AI 궁합 결과")
class GroupCompatibility(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("그룹 궁합 고유 ID")
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @Comment("소속 그룹")
    val group: Group,

    @ManyToOne(fetch = FetchType.LAZY)
    @Comment("첫 번째 사용자 (userA.id < userB.id)")
    val userA: User,

    @ManyToOne(fetch = FetchType.LAZY)
    @Comment("두 번째 사용자 (userA.id < userB.id)")
    val userB: User,

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
