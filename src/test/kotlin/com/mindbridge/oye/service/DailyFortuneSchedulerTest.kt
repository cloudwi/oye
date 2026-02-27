package com.mindbridge.oye.service

import com.mindbridge.oye.domain.CalendarType
import com.mindbridge.oye.domain.Compatibility
import com.mindbridge.oye.domain.Fortune
import com.mindbridge.oye.domain.RelationType
import com.mindbridge.oye.domain.User
import com.mindbridge.oye.domain.UserConnection
import com.mindbridge.oye.repository.UserConnectionRepository
import com.mindbridge.oye.repository.UserRepository
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import java.time.LocalDate

@ExtendWith(MockitoExtension::class)
class DailyFortuneSchedulerTest {

    @Mock
    private lateinit var userRepository: UserRepository

    @Mock
    private lateinit var userConnectionRepository: UserConnectionRepository

    @Mock
    private lateinit var fortuneService: FortuneService

    @Mock
    private lateinit var compatibilityService: CompatibilityService

    @InjectMocks
    private lateinit var scheduler: DailyFortuneScheduler

    private fun createUser(id: Long): User = User(
        id = id,
        name = "유저$id",
        birthDate = LocalDate.of(1990, 1, 1),
        calendarType = CalendarType.SOLAR
    )

    // === generateDailyFortunes ===

    @Test
    fun `generateDailyFortunes - 유저가 없으면 아무것도 하지 않는다`() {
        val emptyPage = PageImpl<User>(emptyList(), PageRequest.of(0, 50), 0)
        whenever(userRepository.findAll(any<Pageable>())).thenReturn(emptyPage)

        scheduler.generateDailyFortunes()

        verify(fortuneService, never()).generateFortune(any())
    }

    @Test
    fun `generateDailyFortunes - 모든 유저에 대해 예감을 생성한다`() {
        val users = (1L..3L).map { createUser(it) }
        val page = PageImpl(users, PageRequest.of(0, 50), 3)
        whenever(userRepository.findAll(any<Pageable>())).thenReturn(page)

        scheduler.generateDailyFortunes()

        users.forEach { user ->
            verify(fortuneService).generateFortune(user)
        }
    }

    @Test
    fun `generateDailyFortunes - 개별 유저 실패가 다른 유저에 영향을 주지 않는다`() {
        val user1 = createUser(1L)
        val user2 = createUser(2L)
        val user3 = createUser(3L)
        val users = listOf(user1, user2, user3)
        val page = PageImpl(users, PageRequest.of(0, 50), 3)
        whenever(userRepository.findAll(any<Pageable>())).thenReturn(page)
        doAnswer { invocation ->
            val user = invocation.getArgument<User>(0)
            if (user.id == 2L) throw RuntimeException("AI 오류")
            null
        }.whenever(fortuneService).generateFortune(any())

        scheduler.generateDailyFortunes()

        verify(fortuneService).generateFortune(user1)
        verify(fortuneService).generateFortune(user2)
        verify(fortuneService).generateFortune(user3)
    }

    @Test
    fun `generateDailyFortunes - 배치 단위로 페이징 처리한다`() {
        // 첫 번째 배치: 50명
        val batch1Users = (1L..50L).map { createUser(it) }
        val page1 = PageImpl(batch1Users, PageRequest.of(0, 50), 75)
        // 두 번째 배치: 25명
        val batch2Users = (51L..75L).map { createUser(it) }
        val page2 = PageImpl(batch2Users, PageRequest.of(1, 50), 75)

        whenever(userRepository.findAll(eq(PageRequest.of(0, 50)))).thenReturn(page1)
        whenever(userRepository.findAll(eq(PageRequest.of(1, 50)))).thenReturn(page2)

        scheduler.generateDailyFortunes()

        verify(fortuneService, times(75)).generateFortune(any())
        verify(userRepository).findAll(eq(PageRequest.of(0, 50)))
        verify(userRepository).findAll(eq(PageRequest.of(1, 50)))
    }

    @Test
    fun `generateDailyFortunes - 모든 유저가 실패해도 예외 없이 완료된다`() {
        val users = (1L..3L).map { createUser(it) }
        val page = PageImpl(users, PageRequest.of(0, 50), 3)
        whenever(userRepository.findAll(any<Pageable>())).thenReturn(page)
        doThrow(RuntimeException("실패")).whenever(fortuneService).generateFortune(any())

        // 예외 없이 정상 완료되어야 한다
        scheduler.generateDailyFortunes()

        verify(fortuneService, times(3)).generateFortune(any())
    }

    // === generateDailyCompatibilities ===

    @Test
    fun `generateDailyCompatibilities - 연결이 없으면 아무것도 하지 않는다`() {
        whenever(userConnectionRepository.findAllWithUsers()).thenReturn(emptyList())

        scheduler.generateDailyCompatibilities()

        verify(compatibilityService, never()).generateCompatibility(any())
    }

    @Test
    fun `generateDailyCompatibilities - 모든 연결에 대해 궁합을 생성한다`() {
        val user1 = createUser(1L)
        val user2 = createUser(2L)
        val user3 = createUser(3L)
        val conn1 = UserConnection(id = 1L, user = user1, partner = user2, relationType = RelationType.FRIEND)
        val conn2 = UserConnection(id = 2L, user = user1, partner = user3, relationType = RelationType.FAMILY)
        whenever(userConnectionRepository.findAllWithUsers()).thenReturn(listOf(conn1, conn2))

        scheduler.generateDailyCompatibilities()

        verify(compatibilityService).generateCompatibility(conn1)
        verify(compatibilityService).generateCompatibility(conn2)
    }

    @Test
    fun `generateDailyCompatibilities - 개별 연결 실패가 다른 연결에 영향을 주지 않는다`() {
        val user1 = createUser(1L)
        val user2 = createUser(2L)
        val user3 = createUser(3L)
        val conn1 = UserConnection(id = 1L, user = user1, partner = user2, relationType = RelationType.FRIEND)
        val conn2 = UserConnection(id = 2L, user = user1, partner = user3, relationType = RelationType.FAMILY)
        whenever(userConnectionRepository.findAllWithUsers()).thenReturn(listOf(conn1, conn2))
        doThrow(RuntimeException("궁합 생성 오류")).whenever(compatibilityService).generateCompatibility(conn1)

        scheduler.generateDailyCompatibilities()

        verify(compatibilityService).generateCompatibility(conn1)
        verify(compatibilityService).generateCompatibility(conn2)
    }

    @Test
    fun `generateDailyCompatibilities - 모든 연결이 실패해도 예외 없이 완료된다`() {
        val user1 = createUser(1L)
        val user2 = createUser(2L)
        val connections = listOf(
            UserConnection(id = 1L, user = user1, partner = user2, relationType = RelationType.LOVER)
        )
        whenever(userConnectionRepository.findAllWithUsers()).thenReturn(connections)
        doThrow(RuntimeException("실패")).whenever(compatibilityService).generateCompatibility(any())

        scheduler.generateDailyCompatibilities()

        verify(compatibilityService, times(1)).generateCompatibility(any())
    }
}
