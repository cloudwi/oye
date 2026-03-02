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
import org.hibernate.annotations.Comment
import java.time.LocalDateTime

@Entity
@Table(name = "user_groups")
@Comment("그룹 정보")
class Group(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("그룹 고유 ID")
    val id: Long? = null,

    @Column(length = 50, nullable = false)
    @Comment("그룹 이름")
    var name: String,

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    @Comment("관계 유형 (FRIEND, FAMILY, COLLEAGUE)")
    val relationType: RelationType,

    @ManyToOne(fetch = FetchType.LAZY)
    @Comment("그룹 생성자")
    var owner: User,

    @Column(unique = true, length = 6, nullable = false)
    @Comment("그룹 초대 코드 (6자리 영숫자)")
    val inviteCode: String,

    @Column(nullable = false, updatable = false)
    @Comment("그룹 생성일시")
    val createdAt: LocalDateTime = LocalDateTime.now()
)
