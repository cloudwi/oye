package com.mindbridge.oye.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.Comment
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "lotto_rounds")
@Comment("로또 회차 당첨 번호 정보")
class LottoRound(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("로또 회차 고유 ID")
    val id: Long? = null,

    @Column(nullable = false, unique = true)
    @Comment("회차 번호")
    val round: Int,

    @Column(nullable = false)
    @Comment("당첨 번호 1")
    val number1: Int,

    @Column(nullable = false)
    @Comment("당첨 번호 2")
    val number2: Int,

    @Column(nullable = false)
    @Comment("당첨 번호 3")
    val number3: Int,

    @Column(nullable = false)
    @Comment("당첨 번호 4")
    val number4: Int,

    @Column(nullable = false)
    @Comment("당첨 번호 5")
    val number5: Int,

    @Column(nullable = false)
    @Comment("당첨 번호 6")
    val number6: Int,

    @Column(nullable = false)
    @Comment("보너스 번호")
    val bonusNumber: Int,

    @Column(nullable = false)
    @Comment("추첨일")
    val drawDate: LocalDate,

    @Column(nullable = false, updatable = false)
    @Comment("생성일시")
    val createdAt: LocalDateTime = LocalDateTime.now()
) {
    val numbers: List<Int>
        get() = listOf(number1, number2, number3, number4, number5, number6)
}
