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
@Table(name = "login_history")
@Comment("로그인 이력")
class LoginHistory(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("로그인 이력 고유 ID")
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @Comment("사용자")
    val user: User,

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    @Comment("소셜 로그인 제공자")
    val provider: SocialProvider,

    @Column(length = 45)
    @Comment("IP 주소")
    val ipAddress: String? = null,

    @Column(length = 500)
    @Comment("User-Agent")
    val userAgent: String? = null,

    @Column(nullable = false, updatable = false)
    @Comment("로그인 시각")
    val createdAt: LocalDateTime = LocalDateTime.now()
)
