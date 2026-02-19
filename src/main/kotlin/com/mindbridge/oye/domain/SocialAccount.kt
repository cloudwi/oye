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
import jakarta.persistence.UniqueConstraint
import org.hibernate.annotations.Comment
import java.time.LocalDateTime

@Entity
@Table(
    name = "social_accounts",
    uniqueConstraints = [UniqueConstraint(columnNames = ["provider", "provider_id"])]
)
@Comment("소셜 로그인 계정 정보")
class SocialAccount(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("소셜 계정 고유 ID")
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @Comment("사용자 FK")
    val user: User,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Comment("소셜 로그인 제공자 (KAKAO, APPLE)")
    val provider: SocialProvider,

    @Column(name = "provider_id", nullable = false)
    @Comment("소셜 로그인 제공자의 고유 사용자 ID")
    val providerId: String,

    @Column(nullable = false, updatable = false)
    @Comment("연동일시")
    val createdAt: LocalDateTime = LocalDateTime.now()
)
