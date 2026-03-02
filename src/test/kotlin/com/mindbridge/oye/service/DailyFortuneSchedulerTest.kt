package com.mindbridge.oye.service

import com.mindbridge.oye.domain.CalendarType
import com.mindbridge.oye.domain.Compatibility
import com.mindbridge.oye.domain.Fortune
import com.mindbridge.oye.domain.RelationType
import com.mindbridge.oye.domain.User
import com.mindbridge.oye.domain.UserConnection
import com.mindbridge.oye.repository.GroupRepository
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
    private lateinit var groupRepository: GroupRepository

    @Mock
    private lateinit var fortuneService: FortuneService

    @Mock
    private lateinit var compatibilityService: CompatibilityService

    @Mock
    private lateinit var groupCompatibilityService: GroupCompatibilityService

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

        verify(fortuneService, never()).generateFortune(any(), any())
    }

    @Test
    fun `generateDailyFortunes - 모든 유저에 대해 예감을 생성한다`() {
        val users = (1L..3L).map { createUser(it) }
        val page = PageImpl(users, PageRequest.of(0, 50), 3)
        whenever(userRepository.findAll(any<Pageable>())).thenReturn(page)

        scheduler.generateDailyFortunes()

        users.forEach { user ->
            verify(fortuneService).generateFortune(eq(user), any())
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
        }.whenever(fortuneService).generateFortune(any(), any())

        scheduler.generateDailyFortunes()

        verify(fortuneService).generateFortune(eq(user1), any())
        verify(fortuneService).generateFortune(eq(user2), any())
        verify(fortuneService).generateFortune(eq(user3), any())
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

        verify(fortuneService, times(75)).generateFortune(any(), any())
        verify(userRepository).findAll(eq(PageRequest.of(0, 50)))
        verify(userRepository).findAll(eq(PageRequest.of(1, 50)))
    }

    @Test
    fun `generateDailyFortunes - 모든 유저가 실패해도 예외 없이 완료된다`() {
        val users = (1L..3L).map { createUser(it) }
        val page = PageImpl(users, PageRequest.of(0, 50), 3)
        whenever(userRepository.findAll(any<Pageable>())).thenReturn(page)
        doThrow(RuntimeException("실패")).whenever(fortuneService).generateFortune(any(), any())

        // 예외 없이 정상 완료되어야 한다
        scheduler.generateDailyFortunes()

        verify(fortuneService, times(3)).generateFortune(any(), any())
    }

    // === generateDailyCompatibilities ===

    @Test
    fun `generateDailyCompatibilities - 연결이 없으면 아무것도 하지 않는다`() {
        val emptyPage = PageImpl<UserConnection>(emptyList(), PageRequest.of(0, 50), 0)
        whenever(userConnectionRepository.findAllWithUsers(any<Pageable>())).thenReturn(emptyPage)

        scheduler.generateDailyCompatibilities()

        verify(compatibilityService, never()).generateCompatibility(any(), any())
    }

    @Test
    fun `generateDailyCompatibilities - 모든 연결에 대해 궁합을 생성한다`() {
        val user1 = createUser(1L)
        val user2 = createUser(2L)
        val user3 = createUser(3L)
        val conn1 = UserConnection(id = 1L, user = user1, partner = user2, relationType = RelationType.FRIEND)
        val conn2 = UserConnection(id = 2L, user = user1, partner = user3, relationType = RelationType.FAMILY)
        val page = PageImpl(listOf(conn1, conn2), PageRequest.of(0, 50), 2)
        whenever(userConnectionRepository.findAllWithUsers(any<Pageable>())).thenReturn(page)

        scheduler.generateDailyCompatibilities()

        verify(compatibilityService).generateCompatibility(eq(conn1), any())
        verify(compatibilityService).generateCompatibility(eq(conn2), any())
    }

    @Test
    fun `generateDailyCompatibilities - 개별 연결 실패가 다른 연결에 영향을 주지 않는다`() {
        val user1 = createUser(1L)
        val user2 = createUser(2L)
        val user3 = createUser(3L)
        val conn1 = UserConnection(id = 1L, user = user1, partner = user2, relationType = RelationType.FRIEND)
        val conn2 = UserConnection(id = 2L, user = user1, partner = user3, relationType = RelationType.FAMILY)
        val page = PageImpl(listOf(conn1, conn2), PageRequest.of(0, 50), 2)
        whenever(userConnectionRepository.findAllWithUsers(any<Pageable>())).thenReturn(page)
        doThrow(RuntimeException("궁합 생성 오류")).whenever(compatibilityService).generateCompatibility(eq(conn1), any())

        scheduler.generateDailyCompatibilities()

        verify(compatibilityService).generateCompatibility(eq(conn1), any())
        verify(compatibilityService).generateCompatibility(eq(conn2), any())
    }

    @Test
    fun `generateDailyCompatibilities - 모든 연결이 실패해도 예외 없이 완료된다`() {
        val user1 = createUser(1L)
        val user2 = createUser(2L)
        val connections = listOf(
            UserConnection(id = 1L, user = user1, partner = user2, relationType = RelationType.LOVER)
        )
        val page = PageImpl(connections, PageRequest.of(0, 50), 1)
        whenever(userConnectionRepository.findAllWithUsers(any<Pageable>())).thenReturn(page)
        doThrow(RuntimeException("실패")).whenever(compatibilityService).generateCompatibility(any(), any())

        scheduler.generateDailyCompatibilities()

        verify(compatibilityService, times(1)).generateCompatibility(any(), any())
    }
}
