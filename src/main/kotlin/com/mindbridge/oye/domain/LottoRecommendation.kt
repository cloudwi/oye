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
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.hibernate.annotations.Comment
import java.time.LocalDateTime

@Entity
@Table(
    name = "lotto_recommendations",
    uniqueConstraints = [UniqueConstraint(columnNames = ["user_id", "round", "set_number"])],
    indexes = [Index(name = "idx_lotto_rec_user_rank", columnList = "user_id, rank")]
)
@Comment("유저별 로또 추천 번호")
class LottoRecommendation(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("추천 고유 ID")
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @Comment("추천 대상 사용자")
    val user: User,

    @Column(nullable = false)
    @Comment("회차 번호")
    val round: Int,

    @Column(nullable = false)
    @Comment("세트 번호 (1~5)")
    val setNumber: Int,

    @Column(nullable = false)
    @Comment("추천 번호 1")
    val number1: Int,

    @Column(nullable = false)
    @Comment("추천 번호 2")
    val number2: Int,

    @Column(nullable = false)
    @Comment("추천 번호 3")
    val number3: Int,

    @Column(nullable = false)
    @Comment("추천 번호 4")
    val number4: Int,

    @Column(nullable = false)
    @Comment("추천 번호 5")
    val number5: Int,

    @Column(nullable = false)
    @Comment("추천 번호 6")
    val number6: Int,

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    @Comment("당첨 등수")
    var rank: LottoRank? = null,

    @Column(nullable = false)
    @Comment("당첨 번호 일치 개수")
    var matchCount: Int = 0,

    @Column(nullable = false)
    @Comment("보너스 번호 일치 여부")
    var bonusMatch: Boolean = false,

    @Column(nullable = false)
    @Comment("당첨 평가 완료 여부")
    var evaluated: Boolean = false,

    @Comment("당첨 금액")
    var prizeAmount: Long? = null,

    @Column(nullable = false, updatable = false)
    @Comment("생성일시")
    val createdAt: LocalDateTime = LocalDateTime.now()
) {
    val numbers: List<Int>
        get() = listOf(number1, number2, number3, number4, number5, number6)
}
