package com.mindbridge.oye.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
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
    name = "group_members",
    uniqueConstraints = [UniqueConstraint(columnNames = ["group_id", "user_id"])]
)
@Comment("그룹 멤버")
class GroupMember(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("그룹 멤버 고유 ID")
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @Comment("소속 그룹")
    val group: Group,

    @ManyToOne(fetch = FetchType.LAZY)
    @Comment("멤버 사용자")
    val user: User,

    @Column(nullable = false, updatable = false)
    @Comment("그룹 가입일시")
    val joinedAt: LocalDateTime = LocalDateTime.now()
)
