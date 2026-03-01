package com.mindbridge.oye.service

import com.mindbridge.oye.domain.CalendarType
import com.mindbridge.oye.domain.Compatibility
import com.mindbridge.oye.domain.Gender
import com.mindbridge.oye.domain.RelationType
import com.mindbridge.oye.domain.User
import com.mindbridge.oye.domain.UserConnection
import com.mindbridge.oye.exception.ConnectionNotFoundException
import com.mindbridge.oye.exception.ForbiddenException
import com.mindbridge.oye.repository.CompatibilityRepository
import com.mindbridge.oye.repository.UserConnectionRepository
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
import org.mockito.kotlin.whenever
import org.springframework.ai.chat.client.ChatClient
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.time.LocalDate
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class CompatibilityServiceTest {

    @Mock
    private lateinit var compatibilityRepository: CompatibilityRepository

    @Mock
    private lateinit var userConnectionRepository: UserConnectionRepository

    @Mock
    private lateinit var chatClientBuilder: ChatClient.Builder

    @Mock
    private lateinit var chatClient: ChatClient

    private val testUser = User(
        id = 1L,
        name = "테스트유저",
        birthDate = LocalDate.of(1990, 1, 15),
        gender = Gender.MALE,
        calendarType = CalendarType.SOLAR
    )

    private val partnerUser = User(
        id = 2L,
        name = "파트너유저",
        birthDate = LocalDate.of(1992, 5, 20),
        gender = Gender.FEMALE,
        calendarType = CalendarType.LUNAR
    )

    private val connection = UserConnection(
        id = 1L,
        user = testUser,
        partner = partnerUser,
        relationType = RelationType.LOVER
    )

    private fun createService(): CompatibilityService {
        whenever(chatClientBuilder.build()).thenReturn(chatClient)
        return CompatibilityService(chatClientBuilder, compatibilityRepository, userConnectionRepository)
    }

    @Test
    fun `getTodayCompatibility - 오늘 궁합이 있으면 반환한다`() {
        val service = createService()
        val compatibility = Compatibility(
            id = 1L,
            connection = connection,
            score = 85,
            content = "오늘 두 분의 궁합은 좋아요.",
            date = LocalDate.now()
        )
        whenever(compatibilityRepository.findByConnectionAndDate(connection, LocalDate.now()))
            .thenReturn(compatibility)

        val result = service.getTodayCompatibility(connection)

        assertNotNull(result)
        assertEquals(85, result!!.score)
    }

    @Test
    fun `getTodayCompatibility - 오늘 궁합이 없으면 null 반환`() {
        val service = createService()
        whenever(compatibilityRepository.findByConnectionAndDate(connection, LocalDate.now()))
            .thenReturn(null)

        val result = service.getTodayCompatibility(connection)

        assertNull(result)
    }

    @Test
    fun `getCompatibility - 연결을 찾을 수 없으면 ConnectionNotFoundException 발생`() {
        val service = createService()
        whenever(userConnectionRepository.findByIdWithUsers(999L)).thenReturn(Optional.empty())

        assertThrows<ConnectionNotFoundException> {
            service.getCompatibility(testUser, 999L)
        }
    }

    @Test
    fun `getCompatibility - 권한이 없으면 ForbiddenException 발생`() {
        val service = createService()
        val otherUser = User(
            id = 3L,
            name = "다른유저",
            birthDate = LocalDate.of(1988, 3, 10),
            gender = Gender.MALE,
            calendarType = CalendarType.SOLAR
        )
        whenever(userConnectionRepository.findByIdWithUsers(1L)).thenReturn(Optional.of(connection))

        assertThrows<ForbiddenException> {
            service.getCompatibility(otherUser, 1L)
        }
    }

    @Test
    fun `getCompatibility - 기존 궁합이 있으면 캐시된 결과를 반환한다`() {
        val service = createService()
        val existing = Compatibility(
            id = 1L,
            connection = connection,
            score = 90,
            content = "오늘은 매우 좋은 궁합이에요.",
            date = LocalDate.now()
        )
        whenever(userConnectionRepository.findByIdWithUsers(1L)).thenReturn(Optional.of(connection))
        whenever(compatibilityRepository.findByConnectionAndDate(connection, LocalDate.now()))
            .thenReturn(existing)

        val result = service.getCompatibility(testUser, 1L)

        assertEquals(90, result.score)
        assertEquals("오늘은 매우 좋은 궁합이에요.", result.content)
    }

    @Test
    fun `saveCompatibility - 기존 궁합이 있으면 저장하지 않고 반환한다`() {
        val service = createService()
        val existing = Compatibility(
            id = 1L,
            connection = connection,
            score = 85,
            content = "기존 궁합",
            date = LocalDate.now()
        )
        whenever(compatibilityRepository.findByConnectionAndDate(connection, LocalDate.now()))
            .thenReturn(existing)

        val result = service.saveCompatibility(connection, 90, "새 궁합")

        assertEquals(85, result.score)
        assertEquals("기존 궁합", result.content)
    }

    @Test
    fun `saveCompatibility - 기존 궁합이 없으면 새로 저장한다`() {
        val service = createService()
        whenever(compatibilityRepository.findByConnectionAndDate(connection, LocalDate.now()))
            .thenReturn(null)
        whenever(compatibilityRepository.save(any<Compatibility>())).thenAnswer { invocation ->
            val comp = invocation.getArgument<Compatibility>(0)
            Compatibility(
                id = 1L,
                connection = comp.connection,
                score = comp.score,
                content = comp.content,
                date = comp.date
            )
        }

        val result = service.saveCompatibility(connection, 75, "새 궁합 내용")

        assertEquals(75, result.score)
        assertEquals("새 궁합 내용", result.content)
    }

    @Test
    fun `getScoreTrend - 점수 추이를 반환한다`() {
        val service = createService()
        val today = LocalDate.now()
        val compatibilities = listOf(
            Compatibility(id = 1L, connection = connection, score = 80, content = "궁합1", date = today.minusDays(1)),
            Compatibility(id = 2L, connection = connection, score = 90, content = "궁합2", date = today)
        )
        whenever(userConnectionRepository.findByIdWithUsers(1L)).thenReturn(Optional.of(connection))
        whenever(compatibilityRepository.findByConnectionAndDateBetweenOrderByDateAsc(any(), any(), any()))
            .thenReturn(compatibilities)

        val result = service.getScoreTrend(testUser, 1L, 7)

        assertEquals(2, result.size)
        assertEquals(80, result[0].score)
        assertEquals(90, result[1].score)
    }

    @Test
    fun `getScoreTrend - 연결을 찾을 수 없으면 ConnectionNotFoundException 발생`() {
        val service = createService()
        whenever(userConnectionRepository.findByIdWithUsers(999L)).thenReturn(Optional.empty())

        assertThrows<ConnectionNotFoundException> {
            service.getScoreTrend(testUser, 999L, 7)
        }
    }

    @Test
    fun `getScoreTrend - 권한이 없으면 ForbiddenException 발생`() {
        val service = createService()
        val otherUser = User(
            id = 3L,
            name = "다른유저",
            birthDate = LocalDate.of(1988, 3, 10),
            gender = Gender.MALE,
            calendarType = CalendarType.SOLAR
        )
        whenever(userConnectionRepository.findByIdWithUsers(1L)).thenReturn(Optional.of(connection))

        assertThrows<ForbiddenException> {
            service.getScoreTrend(otherUser, 1L, 7)
        }
    }

    @Test
    fun `getRecordDates - 기록 날짜를 반환한다`() {
        val service = createService()
        val dates = listOf(LocalDate.of(2026, 3, 5), LocalDate.of(2026, 3, 10))
        whenever(userConnectionRepository.findByIdWithUsers(1L)).thenReturn(Optional.of(connection))
        whenever(compatibilityRepository.findDatesByConnectionAndDateBetween(any(), any(), any()))
            .thenReturn(dates)

        val result = service.getRecordDates(testUser, 1L, 2026, 3)

        assertEquals("2026-03", result.yearMonth)
        assertEquals(2, result.dates.size)
    }

    @Test
    fun `getRecordDates - 연결을 찾을 수 없으면 ConnectionNotFoundException 발생`() {
        val service = createService()
        whenever(userConnectionRepository.findByIdWithUsers(999L)).thenReturn(Optional.empty())

        assertThrows<ConnectionNotFoundException> {
            service.getRecordDates(testUser, 999L, 2026, 3)
        }
    }

    @Test
    fun `getRecordDates - 권한이 없으면 ForbiddenException 발생`() {
        val service = createService()
        val otherUser = User(
            id = 3L,
            name = "다른유저",
            birthDate = LocalDate.of(1988, 3, 10),
            gender = Gender.MALE,
            calendarType = CalendarType.SOLAR
        )
        whenever(userConnectionRepository.findByIdWithUsers(1L)).thenReturn(Optional.of(connection))

        assertThrows<ForbiddenException> {
            service.getRecordDates(otherUser, 1L, 2026, 3)
        }
    }

    @Test
    fun `getCompatibilityHistory - 연결을 찾을 수 없으면 ConnectionNotFoundException 발생`() {
        val service = createService()
        whenever(userConnectionRepository.findByIdWithUsers(999L)).thenReturn(Optional.empty())

        assertThrows<ConnectionNotFoundException> {
            service.getCompatibilityHistory(testUser, 999L, 0, 20)
        }
    }

    @Test
    fun `getCompatibilityHistory - 권한이 없으면 ForbiddenException 발생`() {
        val service = createService()
        val otherUser = User(
            id = 3L,
            name = "다른유저",
            birthDate = LocalDate.of(1988, 3, 10),
            gender = Gender.MALE,
            calendarType = CalendarType.SOLAR
        )
        whenever(userConnectionRepository.findByIdWithUsers(1L)).thenReturn(Optional.of(connection))

        assertThrows<ForbiddenException> {
            service.getCompatibilityHistory(otherUser, 1L, 0, 20)
        }
    }

    @Test
    fun `getCompatibilityHistory - 페이지네이션 결과를 반환한다`() {
        val service = createService()
        val compatibilities = listOf(
            Compatibility(id = 2L, connection = connection, score = 90, content = "오늘의 궁합", date = LocalDate.now()),
            Compatibility(id = 1L, connection = connection, score = 80, content = "어제의 궁합", date = LocalDate.now().minusDays(1))
        )
        val pageable = PageRequest.of(0, 20)
        val page = PageImpl(compatibilities, pageable, 2)

        whenever(userConnectionRepository.findByIdWithUsers(1L)).thenReturn(Optional.of(connection))
        whenever(compatibilityRepository.findByConnectionOrderByDateDesc(connection, pageable))
            .thenReturn(page)

        val result = service.getCompatibilityHistory(testUser, 1L, 0, 20)

        assertEquals(2, result.content.size)
        assertEquals(0, result.page)
        assertEquals(20, result.size)
        assertEquals(2L, result.totalElements)
        assertEquals(1, result.totalPages)
    }

}
