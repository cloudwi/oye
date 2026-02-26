package com.mindbridge.oye.service

import com.mindbridge.oye.domain.CalendarType
import com.mindbridge.oye.domain.Gender
import com.mindbridge.oye.domain.LottoRank
import com.mindbridge.oye.domain.LottoRecommendation
import com.mindbridge.oye.domain.LottoRound
import com.mindbridge.oye.domain.User
import com.mindbridge.oye.repository.LottoRecommendationRepository
import com.mindbridge.oye.repository.LottoRoundRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDate

@ExtendWith(MockitoExtension::class)
class LottoDrawServiceTest {

    @Mock
    private lateinit var lottoRoundRepository: LottoRoundRepository

    @Mock
    private lateinit var lottoRecommendationRepository: LottoRecommendationRepository

    @InjectMocks
    private lateinit var lottoDrawService: LottoDrawService

    private val testUser = User(
        id = 1L,
        name = "테스트유저",
        birthDate = LocalDate.of(1990, 1, 15),
        gender = Gender.MALE,
        calendarType = CalendarType.SOLAR
    )

    private val lottoRound = LottoRound(
        id = 1L, round = 1130,
        number1 = 3, number2 = 11, number3 = 19, number4 = 25, number5 = 33, number6 = 42,
        bonusNumber = 7, drawDate = LocalDate.of(2025, 6, 14)
    )

    @Test
    fun `evaluateRecommendations - 6개 일치 시 1등`() {
        val recommendation = LottoRecommendation(
            id = 1L, user = testUser, round = 1130, setNumber = 1,
            number1 = 3, number2 = 11, number3 = 19, number4 = 25, number5 = 33, number6 = 42
        )
        whenever(lottoRecommendationRepository.findByRound(1130))
            .thenReturn(listOf(recommendation))
        whenever(lottoRecommendationRepository.saveAll(any<List<LottoRecommendation>>()))
            .thenReturn(listOf(recommendation))

        lottoDrawService.evaluateRecommendations(lottoRound)

        assertEquals(6, recommendation.matchCount)
        assertEquals(LottoRank.FIRST, recommendation.rank)
    }

    @Test
    fun `evaluateRecommendations - 5개 일치 + 보너스 일치 시 2등`() {
        val recommendation = LottoRecommendation(
            id = 1L, user = testUser, round = 1130, setNumber = 1,
            number1 = 3, number2 = 7, number3 = 11, number4 = 19, number5 = 25, number6 = 33
        )
        whenever(lottoRecommendationRepository.findByRound(1130))
            .thenReturn(listOf(recommendation))
        whenever(lottoRecommendationRepository.saveAll(any<List<LottoRecommendation>>()))
            .thenReturn(listOf(recommendation))

        lottoDrawService.evaluateRecommendations(lottoRound)

        assertEquals(5, recommendation.matchCount)
        assertEquals(true, recommendation.bonusMatch)
        assertEquals(LottoRank.SECOND, recommendation.rank)
    }

    @Test
    fun `evaluateRecommendations - 5개 일치 + 보너스 불일치 시 3등`() {
        val recommendation = LottoRecommendation(
            id = 1L, user = testUser, round = 1130, setNumber = 1,
            number1 = 3, number2 = 8, number3 = 11, number4 = 19, number5 = 25, number6 = 33
        )
        whenever(lottoRecommendationRepository.findByRound(1130))
            .thenReturn(listOf(recommendation))
        whenever(lottoRecommendationRepository.saveAll(any<List<LottoRecommendation>>()))
            .thenReturn(listOf(recommendation))

        lottoDrawService.evaluateRecommendations(lottoRound)

        assertEquals(5, recommendation.matchCount)
        assertEquals(false, recommendation.bonusMatch)
        assertEquals(LottoRank.THIRD, recommendation.rank)
    }

    @Test
    fun `evaluateRecommendations - 4개 일치 시 4등`() {
        val recommendation = LottoRecommendation(
            id = 1L, user = testUser, round = 1130, setNumber = 1,
            number1 = 3, number2 = 8, number3 = 9, number4 = 19, number5 = 25, number6 = 33
        )
        whenever(lottoRecommendationRepository.findByRound(1130))
            .thenReturn(listOf(recommendation))
        whenever(lottoRecommendationRepository.saveAll(any<List<LottoRecommendation>>()))
            .thenReturn(listOf(recommendation))

        lottoDrawService.evaluateRecommendations(lottoRound)

        assertEquals(4, recommendation.matchCount)
        assertEquals(LottoRank.FOURTH, recommendation.rank)
    }

    @Test
    fun `evaluateRecommendations - 3개 일치 시 5등`() {
        val recommendation = LottoRecommendation(
            id = 1L, user = testUser, round = 1130, setNumber = 1,
            number1 = 3, number2 = 8, number3 = 9, number4 = 10, number5 = 25, number6 = 33
        )
        whenever(lottoRecommendationRepository.findByRound(1130))
            .thenReturn(listOf(recommendation))
        whenever(lottoRecommendationRepository.saveAll(any<List<LottoRecommendation>>()))
            .thenReturn(listOf(recommendation))

        lottoDrawService.evaluateRecommendations(lottoRound)

        assertEquals(3, recommendation.matchCount)
        assertEquals(LottoRank.FIFTH, recommendation.rank)
    }

    @Test
    fun `evaluateRecommendations - 2개 이하 일치 시 당첨 없음`() {
        val recommendation = LottoRecommendation(
            id = 1L, user = testUser, round = 1130, setNumber = 1,
            number1 = 1, number2 = 2, number3 = 4, number4 = 5, number5 = 6, number6 = 8
        )
        whenever(lottoRecommendationRepository.findByRound(1130))
            .thenReturn(listOf(recommendation))
        whenever(lottoRecommendationRepository.saveAll(any<List<LottoRecommendation>>()))
            .thenReturn(listOf(recommendation))

        lottoDrawService.evaluateRecommendations(lottoRound)

        assertNull(recommendation.rank)
    }

    @Test
    fun `evaluateRecommendations - 추천이 없으면 아무 것도 하지 않는다`() {
        whenever(lottoRecommendationRepository.findByRound(1130))
            .thenReturn(emptyList())

        lottoDrawService.evaluateRecommendations(lottoRound)

        verify(lottoRecommendationRepository, never()).saveAll(any<List<LottoRecommendation>>())
    }

    @Test
    fun `fetchDrawResult - 이미 존재하는 회차면 DB 결과를 반환한다`() {
        whenever(lottoRoundRepository.findByRound(1130)).thenReturn(lottoRound)

        val result = lottoDrawService.fetchDrawResult(1130)

        assertEquals(1130, result.round)
        assertEquals(listOf(3, 11, 19, 25, 33, 42), result.numbers)
    }
}
