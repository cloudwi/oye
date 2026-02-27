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
import java.time.LocalDateTime

@Entity
@Table(name = "push_notifications")
@Comment("푸시 알림 발송 이력")
class PushNotification(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Comment("알림 제목")
    val title: String,

    @Column(columnDefinition = "TEXT", nullable = false)
    @Comment("알림 내용")
    val body: String,

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Comment("발송 대상 유형 (ALL: 전체, SPECIFIC: 특정 사용자)")
    val targetType: TargetType,

    @Column(columnDefinition = "TEXT")
    @Comment("대상 사용자 ID 목록 (콤마 구분)")
    val targetUserIds: String? = null,

    @Comment("발송 성공 수")
    var sentCount: Int = 0,

    @Comment("발송 실패 수")
    var failCount: Int = 0,

    @Comment("발송한 관리자 ID")
    val sentBy: Long,

    @Column(nullable = false)
    @Comment("생성일시")
    val createdAt: LocalDateTime = LocalDateTime.now()
)

enum class TargetType {
    ALL,
    SPECIFIC
}
