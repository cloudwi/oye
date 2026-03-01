package com.mindbridge.oye.repository

import com.mindbridge.oye.domain.LottoRecommendation
import com.mindbridge.oye.domain.User
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface LottoRecommendationRepository : JpaRepository<LottoRecommendation, Long> {
    fun findByUserAndRound(user: User, round: Int): List<LottoRecommendation>
    fun findByUserOrderByRoundDescSetNumberAsc(user: User, pageable: Pageable): Page<LottoRecommendation>
    fun findByRoundAndRankIsNotNull(round: Int): List<LottoRecommendation>
    fun findByRankIsNotNullOrderByRoundDescRankAsc(pageable: Pageable): Page<LottoRecommendation>
    fun findByRound(round: Int): List<LottoRecommendation>
    fun findByUserAndRankIsNotNull(user: User): List<LottoRecommendation>
    fun findByUserAndRankIsNotNullOrderByRoundDescSetNumberAsc(user: User, pageable: Pageable): Page<LottoRecommendation>
}
