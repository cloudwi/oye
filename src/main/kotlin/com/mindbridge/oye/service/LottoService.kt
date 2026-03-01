package com.mindbridge.oye.service

import com.mindbridge.oye.domain.LottoRecommendation
import com.mindbridge.oye.domain.User
import com.mindbridge.oye.dto.LottoMyStatsResponse
import com.mindbridge.oye.dto.LottoRecommendationResponse
import com.mindbridge.oye.dto.LottoRoundResponse
import com.mindbridge.oye.dto.LottoWinnerResponse
import com.mindbridge.oye.dto.PageResponse
import com.mindbridge.oye.exception.LottoAlreadyRecommendedException
import com.mindbridge.oye.exception.LottoRecommendationClosedException
import com.mindbridge.oye.exception.LottoRoundNotFoundException
import com.mindbridge.oye.repository.LottoRecommendationRepository
import com.mindbridge.oye.repository.LottoRoundRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

@Service
class LottoService(
    private val lottoRecommendationRepository: LottoRecommendationRepository,
    private val lottoRoundRepository: LottoRoundRepository
) {

    companion object {
        private val LOTTO_EPOCH: LocalDate = LocalDate.of(2002, 12, 7)
        private val KST = ZoneId.of("Asia/Seoul")
        private val DRAW_CUTOFF_TIME = LocalTime.of(20, 30)
        private const val NUMBER_COUNT = 6
        private const val SET_COUNT = 5
        private const val MIN_NUMBER = 1
        private const val MAX_NUMBER = 45
    }

    @Transactional
    fun recommend(user: User, round: Int): List<LottoRecommendationResponse> {
        validateNotInDrawTime()
        val existing = lottoRecommendationRepository.findByUserAndRound(user, round)
        if (existing.isNotEmpty()) {
            throw LottoAlreadyRecommendedException()
        }

        val recommendations = (1..SET_COUNT).map { setNumber ->
            val numbers = generateNumbers()
            LottoRecommendation(
                user = user,
                round = round,
                setNumber = setNumber,
                number1 = numbers[0],
                number2 = numbers[1],
                number3 = numbers[2],
                number4 = numbers[3],
                number5 = numbers[4],
                number6 = numbers[5]
            )
        }

        try {
            val saved = lottoRecommendationRepository.saveAll(recommendations)
            return saved.map { LottoRecommendationResponse.from(it) }
        } catch (e: DataIntegrityViolationException) {
            throw LottoAlreadyRecommendedException()
        }
    }

    @Transactional(readOnly = true)
    fun getMyStats(user: User): LottoMyStatsResponse {
        val winnings = lottoRecommendationRepository.findByUserAndRankIsNotNull(user)
        val totalPrize = winnings.sumOf { it.prizeAmount ?: 0L }
        val winCount = winnings.map { "${it.round}-${it.setNumber}" }.distinct().count()
        return LottoMyStatsResponse(totalPrize = totalPrize, winCount = winCount)
    }

    @Transactional(readOnly = true)
    fun getMyHistory(user: User, page: Int, size: Int, winOnly: Boolean = false): PageResponse<LottoRecommendationResponse> {
        val pageable = PageRequest.of(page, size)
        val recommendationPage = if (winOnly) {
            lottoRecommendationRepository.findByUserAndRankIsNotNullOrderByRoundDescSetNumberAsc(user, pageable)
        } else {
            lottoRecommendationRepository.findByUserOrderByRoundDescSetNumberAsc(user, pageable)
        }

        val evaluatedRounds = recommendationPage.content
            .filter { it.evaluated }
            .map { it.round }
            .distinct()
        val rounds = if (evaluatedRounds.isNotEmpty()) {
            lottoRoundRepository.findByRoundIn(evaluatedRounds).associateBy { it.round }
        } else {
            emptyMap()
        }

        return PageResponse(
            content = recommendationPage.content.map { LottoRecommendationResponse.from(it, rounds[it.round]) },
            page = recommendationPage.number,
            size = recommendationPage.size,
            totalElements = recommendationPage.totalElements,
            totalPages = recommendationPage.totalPages
        )
    }

    @Transactional(readOnly = true)
    fun getWinners(page: Int, size: Int): PageResponse<LottoWinnerResponse> {
        val pageable = PageRequest.of(page, size)
        val winnerPage = lottoRecommendationRepository.findByRankIsNotNullOrderByRoundDescRankAsc(pageable)

        val roundNumbers = winnerPage.content.map { it.round }.distinct()
        val rounds = lottoRoundRepository.findByRoundIn(roundNumbers).associateBy { it.round }

        return PageResponse(
            content = winnerPage.content.map { recommendation ->
                LottoWinnerResponse.from(recommendation, rounds[recommendation.round])
            },
            page = winnerPage.number,
            size = winnerPage.size,
            totalElements = winnerPage.totalElements,
            totalPages = winnerPage.totalPages
        )
    }

    @Transactional(readOnly = true)
    fun getRound(round: Int): LottoRoundResponse {
        val lottoRound = lottoRoundRepository.findByRound(round)
            ?: throw LottoRoundNotFoundException()
        return LottoRoundResponse.from(lottoRound)
    }

    fun getRoundForDate(date: LocalDate): Int {
        val daysSinceEpoch = ChronoUnit.DAYS.between(LOTTO_EPOCH, date)
        return (daysSinceEpoch / 7 + 1).toInt()
    }

    fun getCurrentRound(): Int = getRoundForDate(LocalDate.now())

    private fun validateNotInDrawTime() {
        val now = LocalDateTime.now(KST)
        if (now.dayOfWeek == DayOfWeek.SATURDAY && now.toLocalTime() >= DRAW_CUTOFF_TIME) {
            throw LottoRecommendationClosedException()
        }
    }

    private fun generateNumbers(): List<Int> {
        return (MIN_NUMBER..MAX_NUMBER).shuffled().take(NUMBER_COUNT).sorted()
    }
}
