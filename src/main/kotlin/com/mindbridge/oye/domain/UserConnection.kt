package com.mindbridge.oye.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.hibernate.annotations.Comment
import java.time.LocalDateTime

@Entity
@Table(
    name = "user_connections",
    uniqueConstraints = [UniqueConstraint(columnNames = ["user_id", "partner_id"])]
)
@Comment("사용자 간 연결 (궁합 대상)")
class UserConnection(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("연결 고유 ID")
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @Comment("연결 요청자")
    val user: User,

    @ManyToOne(fetch = FetchType.LAZY)
    @Comment("연결 대상자")
    val partner: User,

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    @Comment("관계 유형 (LOVER, FRIEND, FAMILY, COLLEAGUE)")
    val relationType: RelationType,

    @Column(nullable = false, updatable = false)
    @Comment("연결 생성일시")
    val createdAt: LocalDateTime = LocalDateTime.now()
)
