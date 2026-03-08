package com.mindbridge.oye.service

import com.mindbridge.oye.domain.LottoRecommendation
import com.mindbridge.oye.domain.LottoSource
import com.mindbridge.oye.domain.User
import com.mindbridge.oye.dto.LottoMyStatsResponse
import com.mindbridge.oye.dto.LottoRecommendationResponse
import com.mindbridge.oye.dto.LottoRegisterRequest
import com.mindbridge.oye.dto.LottoRoundResponse
import com.mindbridge.oye.dto.LottoWinnerResponse
import com.mindbridge.oye.dto.PageResponse
import com.mindbridge.oye.exception.LottoAlreadyRecommendedException
import com.mindbridge.oye.exception.LottoInvalidNumbersException
import com.mindbridge.oye.exception.LottoRecommendationClosedException
import com.mindbridge.oye.exception.LottoRegistrationClosedException
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
        val totalPrize = lottoRecommendationRepository.sumPrizeAmountByUser(user)
        val winCount = lottoRecommendationRepository.countWinsByUser(user)
        return LottoMyStatsResponse(totalPrize = totalPrize, winCount = winCount.toInt())
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

    @Transactional
    fun registerNumbers(user: User, request: LottoRegisterRequest): List<LottoRecommendationResponse> {
        if (request.source == LottoSource.AI) {
            throw LottoInvalidNumbersException("AI 소스로는 직접 등록할 수 없습니다.")
        }
        if (request.numberSets.isEmpty() || request.numberSets.size > SET_COUNT) {
            throw LottoInvalidNumbersException("번호 세트는 1~${SET_COUNT}개까지 등록 가능합니다.")
        }
        for (numbers in request.numberSets) {
            if (numbers.size != NUMBER_COUNT) {
                throw LottoInvalidNumbersException("각 세트는 ${NUMBER_COUNT}개의 번호가 필요합니다.")
            }
            if (numbers.any { it < MIN_NUMBER || it > MAX_NUMBER }) {
                throw LottoInvalidNumbersException("번호는 ${MIN_NUMBER}~${MAX_NUMBER} 범위여야 합니다.")
            }
            if (numbers.toSet().size != numbers.size) {
                throw LottoInvalidNumbersException("세트 내 중복 번호가 있습니다.")
            }
        }

        val currentRound = getCurrentRound()
        if (request.round < currentRound) {
            throw LottoRegistrationClosedException("이미 지난 회차에는 등록할 수 없습니다.")
        }

        if (lottoRoundRepository.findByRound(request.round) != null) {
            throw LottoRegistrationClosedException("이미 추첨이 완료된 회차에는 등록할 수 없습니다.")
        }

        validateNotInDrawTime()

        val existingEvaluated = lottoRecommendationRepository.findByUserAndRoundAndSource(user, request.round, request.source)
        if (existingEvaluated.any { it.evaluated }) {
            throw LottoRegistrationClosedException()
        }

        val maxSetNumber = lottoRecommendationRepository.findMaxSetNumberByUserAndRoundAndSource(user, request.round, request.source)
        if (maxSetNumber + request.numberSets.size > SET_COUNT) {
            throw LottoInvalidNumbersException("해당 회차에 최대 ${SET_COUNT}세트까지 등록 가능합니다. (현재 ${maxSetNumber}세트 등록됨)")
        }

        val recommendations = request.numberSets.mapIndexed { index, numbers ->
            val sorted = numbers.sorted()
            LottoRecommendation(
                user = user,
                round = request.round,
                source = request.source,
                setNumber = maxSetNumber + index + 1,
                number1 = sorted[0],
                number2 = sorted[1],
                number3 = sorted[2],
                number4 = sorted[3],
                number5 = sorted[4],
                number6 = sorted[5]
            )
        }

        val saved = lottoRecommendationRepository.saveAll(recommendations)
        return saved.map { LottoRecommendationResponse.from(it) }
    }

    private fun generateNumbers(): List<Int> {
        return (MIN_NUMBER..MAX_NUMBER).shuffled().take(NUMBER_COUNT).sorted()
    }
}
