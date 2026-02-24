package com.mindbridge.oye.service

import com.mindbridge.oye.domain.LottoRecommendation
import com.mindbridge.oye.domain.User
import com.mindbridge.oye.dto.LottoRecommendationResponse
import com.mindbridge.oye.dto.LottoRoundResponse
import com.mindbridge.oye.dto.LottoWinnerResponse
import com.mindbridge.oye.dto.PageResponse
import com.mindbridge.oye.exception.LottoAlreadyRecommendedException
import com.mindbridge.oye.exception.LottoRoundNotFoundException
import com.mindbridge.oye.repository.LottoRecommendationRepository
import com.mindbridge.oye.repository.LottoRoundRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Service
class LottoService(
    private val lottoRecommendationRepository: LottoRecommendationRepository,
    private val lottoRoundRepository: LottoRoundRepository
) {

    companion object {
        private val LOTTO_EPOCH: LocalDate = LocalDate.of(2002, 12, 7)
        private const val NUMBER_COUNT = 6
        private const val SET_COUNT = 5
        private const val MIN_NUMBER = 1
        private const val MAX_NUMBER = 45
    }

    @Transactional
    fun recommend(user: User, round: Int): List<LottoRecommendationResponse> {
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

        val saved = lottoRecommendationRepository.saveAll(recommendations)
        return saved.map { LottoRecommendationResponse.from(it) }
    }

    @Transactional(readOnly = true)
    fun getMyHistory(user: User, page: Int, size: Int): PageResponse<LottoRecommendationResponse> {
        val pageable = PageRequest.of(page, size)
        val recommendationPage = lottoRecommendationRepository.findByUserOrderByRoundDescSetNumberAsc(user, pageable)
        return PageResponse(
            content = recommendationPage.content.map { LottoRecommendationResponse.from(it) },
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

        val rounds = winnerPage.content
            .map { it.round }
            .distinct()
            .let { roundNumbers ->
                roundNumbers.associateWith { lottoRoundRepository.findByRound(it) }
            }

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

    fun getCurrentRound(): Int {
        val today = LocalDate.now()
        val daysSinceEpoch = ChronoUnit.DAYS.between(LOTTO_EPOCH, today)
        return (daysSinceEpoch / 7 + 1).toInt()
    }

    private fun generateNumbers(): List<Int> {
        return (MIN_NUMBER..MAX_NUMBER).shuffled().take(NUMBER_COUNT).sorted()
    }
}
