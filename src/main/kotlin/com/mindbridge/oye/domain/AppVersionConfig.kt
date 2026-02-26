package com.mindbridge.oye.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.Comment
import java.time.LocalDateTime

@Entity
@Table(name = "app_version_config")
@Comment("앱 버전 관리")
class AppVersionConfig(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("고유 ID")
    val id: Long? = null,

    @Column(length = 10, nullable = false, unique = true)
    @Comment("플랫폼 (ios, android)")
    val platform: String,

    @Column(name = "min_version", length = 20, nullable = false)
    @Comment("최소 필수 버전")
    var minVersion: String,

    @Column(name = "store_url", length = 500, nullable = false)
    @Comment("스토어 URL")
    var storeUrl: String,

    @Column(name = "updated_at", nullable = false)
    @Comment("수정일시")
    var updatedAt: LocalDateTime = LocalDateTime.now()
)
