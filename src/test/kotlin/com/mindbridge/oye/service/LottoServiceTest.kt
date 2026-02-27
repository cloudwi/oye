package com.mindbridge.oye.service

import com.mindbridge.oye.domain.CalendarType
import com.mindbridge.oye.domain.Gender
import com.mindbridge.oye.domain.LottoRank
import com.mindbridge.oye.domain.LottoRecommendation
import com.mindbridge.oye.domain.LottoRound
import com.mindbridge.oye.domain.User
import com.mindbridge.oye.exception.LottoAlreadyRecommendedException
import com.mindbridge.oye.exception.LottoRoundNotFoundException
import com.mindbridge.oye.repository.LottoRecommendationRepository
import com.mindbridge.oye.repository.LottoRoundRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.time.LocalDate

@ExtendWith(MockitoExtension::class)
class LottoServiceTest {

    @Mock
    private lateinit var lottoRecommendationRepository: LottoRecommendationRepository

    @Mock
    private lateinit var lottoRoundRepository: LottoRoundRepository

    @InjectMocks
    private lateinit var lottoService: LottoService

    private val testUser = User(
        id = 1L,
        name = "테스트유저",
        birthDate = LocalDate.of(1990, 1, 15),
        gender = Gender.MALE,
        calendarType = CalendarType.SOLAR
    )

    @Test
    fun `recommend - 5세트의 추천 번호를 생성한다`() {
        whenever(lottoRecommendationRepository.findByUserAndRound(testUser, 1130))
            .thenReturn(emptyList())
        whenever(lottoRecommendationRepository.saveAll(any<List<LottoRecommendation>>()))
            .thenAnswer { invocation ->
                val recommendations = invocation.getArgument<List<LottoRecommendation>>(0)
                recommendations.mapIndexed { index, rec ->
                    LottoRecommendation(
                        id = (index + 1).toLong(),
                        user = rec.user,
                        round = rec.round,
                        setNumber = rec.setNumber,
                        number1 = rec.number1,
                        number2 = rec.number2,
                        number3 = rec.number3,
                        number4 = rec.number4,
                        number5 = rec.number5,
                        number6 = rec.number6
                    )
                }
            }

        val result = lottoService.recommend(testUser, 1130)

        assertEquals(5, result.size)
        result.forEachIndexed { index, response ->
            assertEquals(index + 1, response.setNumber)
            assertEquals(1130, response.round)
            assertEquals(6, response.numbers.size)
            assertTrue(response.numbers.all { it in 1..45 })
            assertEquals(response.numbers.sorted(), response.numbers)
        }
    }

    @Test
    fun `recommend - 이미 추천받은 회차이면 LottoAlreadyRecommendedException 발생`() {
        val existing = listOf(
            LottoRecommendation(
                id = 1L, user = testUser, round = 1130, setNumber = 1,
                number1 = 1, number2 = 2, number3 = 3, number4 = 4, number5 = 5, number6 = 6
            )
        )
        whenever(lottoRecommendationRepository.findByUserAndRound(testUser, 1130))
            .thenReturn(existing)

        assertThrows<LottoAlreadyRecommendedException> {
            lottoService.recommend(testUser, 1130)
        }
    }

    @Test
    fun `getMyHistory - 페이지네이션 결과를 반환한다`() {
        val recommendations = (1..3).map { i ->
            LottoRecommendation(
                id = i.toLong(), user = testUser, round = 1130, setNumber = i,
                number1 = 1, number2 = 10, number3 = 20, number4 = 30, number5 = 40, number6 = 45
            )
        }
        val pageable = PageRequest.of(0, 20)
        val page = PageImpl(recommendations, pageable, 3)

        whenever(lottoRecommendationRepository.findByUserOrderByRoundDescSetNumberAsc(testUser, pageable))
            .thenReturn(page)

        val result = lottoService.getMyHistory(testUser, 0, 20)

        assertEquals(3, result.content.size)
        assertEquals(0, result.page)
        assertEquals(20, result.size)
        assertEquals(3L, result.totalElements)
        assertEquals(1, result.totalPages)
    }


    @Test
    fun `getWinners - 당첨자 목록을 반환한다`() {
        val winner = LottoRecommendation(
            id = 1L, user = testUser, round = 1130, setNumber = 1,
            number1 = 1, number2 = 10, number3 = 20, number4 = 30, number5 = 40, number6 = 45,
            rank = LottoRank.FIFTH, matchCount = 3, bonusMatch = false
        )
        val pageable = PageRequest.of(0, 20)
        val page = PageImpl(listOf(winner), pageable, 1)
        val lottoRound = LottoRound(
            id = 1L, round = 1130,
            number1 = 1, number2 = 10, number3 = 20, number4 = 30, number5 = 40, number6 = 45,
            bonusNumber = 7, drawDate = LocalDate.of(2025, 6, 14)
        )

        whenever(lottoRecommendationRepository.findByRankIsNotNullOrderByRoundDescRankAsc(pageable))
            .thenReturn(page)
        whenever(lottoRoundRepository.findByRoundIn(listOf(1130)))
            .thenReturn(listOf(lottoRound))

        val result = lottoService.getWinners(0, 20)

        assertEquals(1, result.content.size)
        assertEquals("5등", result.content[0].rank)
        assertEquals(3, result.content[0].matchCount)
        assertEquals(LocalDate.of(2025, 6, 14), result.content[0].drawDate)
    }


    @Test
    fun `getRound - 회차 정보를 반환한다`() {
        val lottoRound = LottoRound(
            id = 1L, round = 1130,
            number1 = 3, number2 = 11, number3 = 19, number4 = 25, number5 = 33, number6 = 42,
            bonusNumber = 7, drawDate = LocalDate.of(2025, 6, 14)
        )
        whenever(lottoRoundRepository.findByRound(1130)).thenReturn(lottoRound)

        val result = lottoService.getRound(1130)

        assertEquals(1130, result.round)
        assertEquals(listOf(3, 11, 19, 25, 33, 42), result.numbers)
        assertEquals(7, result.bonusNumber)
        assertEquals(LocalDate.of(2025, 6, 14), result.drawDate)
    }

    @Test
    fun `getRound - 존재하지 않는 회차이면 LottoRoundNotFoundException 발생`() {
        whenever(lottoRoundRepository.findByRound(9999)).thenReturn(null)

        assertThrows<LottoRoundNotFoundException> {
            lottoService.getRound(9999)
        }
    }

    @Test
    fun `getRoundForDate - 로또 기준일로부터 회차를 계산한다`() {
        // 2002-12-07 (epoch) + 7일 = 2회차
        val date = LocalDate.of(2002, 12, 14)
        val round = lottoService.getRoundForDate(date)
        assertEquals(2, round)
    }

    @Test
    fun `getRoundForDate - 기준일 당일은 1회차`() {
        val epoch = LocalDate.of(2002, 12, 7)
        val round = lottoService.getRoundForDate(epoch)
        assertEquals(1, round)
    }

    @Test
    fun `getCurrentRound - 현재 날짜 기준 회차를 반환한다`() {
        val round = lottoService.getCurrentRound()
        assertNotNull(round)
        assertTrue(round > 0)
    }
}
