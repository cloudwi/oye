package com.mindbridge.oye.repository

import com.mindbridge.oye.domain.LottoRecommendation
import com.mindbridge.oye.domain.LottoSource
import com.mindbridge.oye.domain.User
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface LottoRecommendationRepository : JpaRepository<LottoRecommendation, Long> {
    fun findByUserAndRound(user: User, round: Int): List<LottoRecommendation>
    fun findByUserOrderByRoundDescSetNumberAsc(user: User, pageable: Pageable): Page<LottoRecommendation>
    fun findByRoundAndRankIsNotNull(round: Int): List<LottoRecommendation>
    @Query(
        value = "SELECT r FROM LottoRecommendation r JOIN FETCH r.user WHERE r.rank IS NOT NULL ORDER BY r.round DESC, r.rank ASC",
        countQuery = "SELECT COUNT(r) FROM LottoRecommendation r WHERE r.rank IS NOT NULL"
    )
    fun findByRankIsNotNullOrderByRoundDescRankAsc(pageable: Pageable): Page<LottoRecommendation>
    fun findByRound(round: Int): List<LottoRecommendation>
    fun findByUserAndRankIsNotNull(user: User): List<LottoRecommendation>
    fun findByUserAndRankIsNotNullOrderByRoundDescSetNumberAsc(user: User, pageable: Pageable): Page<LottoRecommendation>

    fun findByUserIdOrderByRoundDescSetNumberAsc(userId: Long, pageable: Pageable): Page<LottoRecommendation>
    fun deleteAllByUser(user: User)

    @Query("SELECT COALESCE(SUM(r.prizeAmount), 0) FROM LottoRecommendation r WHERE r.user = :user AND r.rank IS NOT NULL")
    fun sumPrizeAmountByUser(user: User): Long

    @Query("SELECT COUNT(r) FROM LottoRecommendation r WHERE r.user = :user AND r.rank IS NOT NULL")
    fun countWinsByUser(user: User): Long

    @Query("SELECT DISTINCT r.round FROM LottoRecommendation r WHERE r.evaluated = false")
    fun findDistinctUnevaluatedRounds(): List<Int>

    @Query("SELECT COALESCE(MAX(r.setNumber), 0) FROM LottoRecommendation r WHERE r.user = :user AND r.round = :round AND r.source = :source")
    fun findMaxSetNumberByUserAndRoundAndSource(user: User, round: Int, source: LottoSource): Int

    fun findByUserAndRoundAndSource(user: User, round: Int, source: LottoSource): List<LottoRecommendation>
}
