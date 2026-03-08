package com.mindbridge.oye.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.Comment
import java.time.LocalDateTime

@Entity
@Table(name = "user_notifications")
@Comment("사용자 알림 수신함")
class UserNotification(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("알림 고유 ID")
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @Comment("대상 사용자")
    val user: User,

    @Column(length = 100, nullable = false)
    @Comment("알림 제목")
    val title: String,

    @Column(columnDefinition = "TEXT", nullable = false)
    @Comment("알림 내용")
    val body: String,

    @Enumerated(EnumType.STRING)
    @Column(length = 30, nullable = false)
    @Comment("알림 유형 (GENERAL, FORTUNE, COMPATIBILITY, CONNECTION, GROUP, LOTTO)")
    val type: NotificationType = NotificationType.GENERAL,

    @Column(nullable = false)
    @Comment("읽음 여부")
    var isRead: Boolean = false,

    @Column(columnDefinition = "TEXT")
    @Comment("추가 메타데이터 (JSON)")
    val metadata: String? = null,

    @Column(nullable = false)
    @Comment("생성일시")
    val createdAt: LocalDateTime = LocalDateTime.now()
)
