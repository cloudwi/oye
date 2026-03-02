package com.mindbridge.oye.service

import com.mindbridge.oye.domain.CalendarType
import com.mindbridge.oye.domain.Fortune
import com.mindbridge.oye.domain.Gender
import com.mindbridge.oye.domain.User
import com.mindbridge.oye.exception.FortuneGenerationException
import com.mindbridge.oye.repository.FortuneRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.time.LocalDate

@ExtendWith(MockitoExtension::class)
class FortuneServiceTest {

    @Mock
    private lateinit var fortuneRepository: FortuneRepository

    @Mock
    private lateinit var aiChatService: AiChatService

    private val testUser = User(
        id = 1L,
        name = "테스트유저",
        birthDate = LocalDate.of(1990, 1, 15),
        gender = Gender.MALE,
        calendarType = CalendarType.SOLAR
    )

    private fun createService(): FortuneService {
        return FortuneService(aiChatService, fortuneRepository)
    }

    @Test
    fun `getTodayFortune - returns cached fortune when it exists`() {
        val service = createService()
        val existingFortune = Fortune(
            id = 1L,
            user = testUser,
            content = "오늘은 좋은 일이 생길 수 있는 날이에요.",
            date = LocalDate.now()
        )
        whenever(fortuneRepository.findByUserAndDate(testUser, LocalDate.now()))
            .thenReturn(existingFortune)

        val result = service.getTodayFortune(testUser)

        assertNotNull(result)
        assertEquals(existingFortune.content, result!!.content)
        assertEquals(existingFortune.id, result.id)
    }

    @Test
    fun `getTodayFortune - returns null when no fortune exists`() {
        val service = createService()
        whenever(fortuneRepository.findByUserAndDate(testUser, LocalDate.now()))
            .thenReturn(null)

        val result = service.getTodayFortune(testUser)

        assertNull(result)
    }

    @Test
    fun `generateFortune - returns existing fortune if already generated today`() {
        val service = createService()
        val existingFortune = Fortune(
            id = 1L,
            user = testUser,
            content = "오늘은 좋은 일이 생길 수 있는 날이에요.",
            date = LocalDate.now()
        )
        whenever(fortuneRepository.findByUserAndDate(testUser, LocalDate.now()))
            .thenReturn(existingFortune)

        val result = service.generateFortune(testUser)

        assertEquals(existingFortune, result)
        verify(fortuneRepository, never()).save(any())
    }

    @Test
    fun `generateFortune - generates new fortune via AI when none exists`() {
        val service = createService()
        val aiContent = "평소 안 보이던 것들이 눈에 들어오는 하루예요."

        whenever(fortuneRepository.findByUserAndDate(testUser, LocalDate.now()))
            .thenReturn(null)
        whenever(aiChatService.callWithRetry<Any>(any(), any(), any(), any())).thenAnswer { invocation ->
            val parser = invocation.getArgument<(String) -> Any>(3)
            parser("""{"content": "$aiContent", "score": 68}""")
        }

        val savedFortune = Fortune(
            id = 2L,
            user = testUser,
            content = aiContent,
            date = LocalDate.now()
        )
        whenever(fortuneRepository.save(any<Fortune>())).thenReturn(savedFortune)

        val result = service.generateFortune(testUser)

        assertEquals(aiContent, result.content)
        assertEquals(testUser, result.user)
        verify(fortuneRepository).save(any<Fortune>())
    }

    @Test
    fun `generateFortune - throws FortuneGenerationException when AI call fails`() {
        val service = createService()
        whenever(fortuneRepository.findByUserAndDate(testUser, LocalDate.now()))
            .thenReturn(null)
        whenever(aiChatService.callWithRetry<Any>(any(), any(), any(), any()))
            .thenThrow(RuntimeException("AI service unavailable"))

        assertThrows<FortuneGenerationException> {
            service.generateFortune(testUser)
        }
    }

    @Test
    fun `getScoreTrend - returns score points with non-null scores`() {
        val service = createService()
        val today = LocalDate.now()
        val fortunes = listOf(
            Fortune(id = 1L, user = testUser, content = "예감1", date = today.minusDays(2), score = 70),
            Fortune(id = 2L, user = testUser, content = "예감2", date = today.minusDays(1), score = null),
            Fortune(id = 3L, user = testUser, content = "예감3", date = today, score = 85)
        )
        whenever(fortuneRepository.findByUserAndDateBetweenOrderByDateAsc(any(), any(), any()))
            .thenReturn(fortunes)

        val result = service.getScoreTrend(testUser, 7)

        assertEquals(2, result.size)
        assertEquals(70, result[0].score)
        assertEquals(85, result[1].score)
    }

    @Test
    fun `getScoreTrend - returns empty list when no fortunes`() {
        val service = createService()
        whenever(fortuneRepository.findByUserAndDateBetweenOrderByDateAsc(any(), any(), any()))
            .thenReturn(emptyList())

        val result = service.getScoreTrend(testUser, 30)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `getRecordDates - returns dates for given month`() {
        val service = createService()
        val dates = listOf(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 15))
        whenever(fortuneRepository.findDatesByUserAndDateBetween(any(), any(), any()))
            .thenReturn(dates)

        val result = service.getRecordDates(testUser, 2026, 3)

        assertEquals("2026-03", result.yearMonth)
        assertEquals(2, result.dates.size)
        assertEquals(LocalDate.of(2026, 3, 1), result.dates[0])
    }

    @Test
    fun `getFortuneHistory - returns paginated results`() {
        val service = createService()
        val fortunes = listOf(
            Fortune(id = 3L, user = testUser, content = "첫번째 예감", date = LocalDate.now()),
            Fortune(id = 2L, user = testUser, content = "두번째 예감", date = LocalDate.now().minusDays(1))
        )
        val pageable = PageRequest.of(0, 20)
        val page = PageImpl(fortunes, pageable, 2)

        whenever(fortuneRepository.findByUserOrderByDateDesc(testUser, pageable))
            .thenReturn(page)

        val result = service.getFortuneHistory(testUser, 0, 20)

        assertEquals(2, result.content.size)
        assertEquals(0, result.page)
        assertEquals(20, result.size)
        assertEquals(2L, result.totalElements)
        assertEquals(1, result.totalPages)
        assertEquals("첫번째 예감", result.content[0].content)
    }

}
