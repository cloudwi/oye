package com.mindbridge.oye.repository

import com.mindbridge.oye.domain.LottoRound
import org.springframework.data.jpa.repository.JpaRepository

interface LottoRoundRepository : JpaRepository<LottoRound, Long> {
    fun findByRound(round: Int): LottoRound?
    fun findTopByOrderByRoundDesc(): LottoRound?
}
