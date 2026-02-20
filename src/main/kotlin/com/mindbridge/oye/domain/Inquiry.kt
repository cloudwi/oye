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
@Table(name = "inquiries")
@Comment("사용자 문의")
class Inquiry(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("문의 고유 ID")
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @Comment("문의 작성자")
    val user: User,

    @Column(length = 100, nullable = false)
    @Comment("문의 제목")
    val title: String,

    @Column(length = 2000, nullable = false)
    @Comment("문의 내용")
    val content: String,

    @Enumerated(EnumType.STRING)
    @Column(length = 10, nullable = false)
    @Comment("문의 상태 (PENDING: 답변 대기, ANSWERED: 답변 완료)")
    var status: InquiryStatus = InquiryStatus.PENDING,

    @Column(length = 2000)
    @Comment("관리자 답변")
    var adminReply: String? = null,

    @Comment("관리자 답변 일시")
    var adminRepliedAt: LocalDateTime? = null,

    @Column(nullable = false, updatable = false)
    @Comment("문의 작성일시")
    val createdAt: LocalDateTime = LocalDateTime.now()
)
