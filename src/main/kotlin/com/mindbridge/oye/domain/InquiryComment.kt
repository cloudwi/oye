package com.mindbridge.oye.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
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
@Table(name = "inquiry_comments")
@Comment("문의 관리자 댓글")
class InquiryComment(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("댓글 고유 ID")
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inquiry_id", nullable = false)
    @Comment("문의")
    val inquiry: Inquiry,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id", nullable = false)
    @Comment("작성 관리자")
    val admin: User,

    @Column(length = 2000, nullable = false)
    @Comment("댓글 내용")
    val content: String,

    @Column(nullable = false, updatable = false)
    @Comment("작성일시")
    val createdAt: LocalDateTime = LocalDateTime.now()
)
