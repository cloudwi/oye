package com.mindbridge.oye.service

import com.mindbridge.oye.domain.CalendarType
import com.mindbridge.oye.domain.Compatibility
import com.mindbridge.oye.domain.Gender
import com.mindbridge.oye.domain.RelationType
import com.mindbridge.oye.domain.User
import com.mindbridge.oye.domain.UserConnection
import com.mindbridge.oye.dto.ConnectRequest
import com.mindbridge.oye.exception.CodeGenerationException
import com.mindbridge.oye.exception.ConnectionNotFoundException
import com.mindbridge.oye.exception.DuplicateConnectionException
import com.mindbridge.oye.exception.ForbiddenException
import com.mindbridge.oye.exception.SelfConnectionException
import com.mindbridge.oye.exception.UserNotFoundException
import com.mindbridge.oye.repository.CompatibilityRepository
import com.mindbridge.oye.repository.UserConnectionRepository
import com.mindbridge.oye.repository.UserRepository
import org.springframework.context.ApplicationEventPublisher
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDate
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class ConnectionServiceTest {

    @Mock
    private lateinit var userRepository: UserRepository

    @Mock
    private lateinit var userConnectionRepository: UserConnectionRepository

    @Mock
    private lateinit var compatibilityRepository: CompatibilityRepository

    @Mock
    private lateinit var eventPublisher: ApplicationEventPublisher

    @InjectMocks
    private lateinit var connectionService: ConnectionService

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
        calendarType = CalendarType.LUNAR,
        connectCode = "ABC123"
    )

    @Test
    fun `getMyCode - 기존 코드가 있으면 그대로 반환한다`() {
        val user = User(
            id = 1L,
            name = "테스트유저",
            birthDate = LocalDate.of(1990, 1, 15),
            gender = Gender.MALE,
            calendarType = CalendarType.SOLAR,
            connectCode = "XYZ789"
        )

        val result = connectionService.getMyCode(user)

        assertEquals("XYZ789", result.code)
        verify(userRepository, never()).save(any())
    }

    @Test
    fun `getMyCode - 코드가 없으면 새로 생성한다`() {
        whenever(userRepository.findByConnectCode(any())).thenReturn(null)
        whenever(userRepository.save(any<User>())).thenReturn(testUser)

        val result = connectionService.getMyCode(testUser)

        assertNotNull(result.code)
        assertEquals(6, result.code.length)
        verify(userRepository).save(testUser)
    }

    @Test
    fun `connect - 정상적으로 연결을 생성한다`() {
        val request = ConnectRequest(code = "ABC123", relationType = RelationType.FRIEND)
        whenever(userRepository.findByConnectCode("ABC123")).thenReturn(partnerUser)
        whenever(
            userConnectionRepository.existsByUserAndPartnerOrPartnerAndUser(
                testUser, partnerUser, testUser, partnerUser
            )
        ).thenReturn(false)
        whenever(userConnectionRepository.save(any<UserConnection>())).thenAnswer { invocation ->
            val conn = invocation.getArgument<UserConnection>(0)
            UserConnection(
                id = 1L,
                user = conn.user,
                partner = conn.partner,
                relationType = conn.relationType
            )
        }

        val result = connectionService.connect(testUser, request)

        assertEquals(1L, result.id)
        assertEquals("파트너유저", result.partnerName)
        assertEquals(RelationType.FRIEND, result.relationType)
    }

    @Test
    fun `connect - 존재하지 않는 초대 코드이면 UserNotFoundException 발생`() {
        val request = ConnectRequest(code = "INVALID", relationType = RelationType.FRIEND)
        whenever(userRepository.findByConnectCode("INVALID")).thenReturn(null)

        assertThrows<UserNotFoundException> {
            connectionService.connect(testUser, request)
        }
    }

    @Test
    fun `connect - 자기 자신에게 연결하면 SelfConnectionException 발생`() {
        val request = ConnectRequest(code = "MYCODE", relationType = RelationType.FRIEND)
        whenever(userRepository.findByConnectCode("MYCODE")).thenReturn(testUser)

        assertThrows<SelfConnectionException> {
            connectionService.connect(testUser, request)
        }
    }

    @Test
    fun `connect - 이미 연결된 사용자면 DuplicateConnectionException 발생`() {
        val request = ConnectRequest(code = "ABC123", relationType = RelationType.FRIEND)
        whenever(userRepository.findByConnectCode("ABC123")).thenReturn(partnerUser)
        whenever(
            userConnectionRepository.existsByUserAndPartnerOrPartnerAndUser(
                testUser, partnerUser, testUser, partnerUser
            )
        ).thenReturn(true)

        assertThrows<DuplicateConnectionException> {
            connectionService.connect(testUser, request)
        }
    }

    @Test
    fun `getMyConnections - 연결 목록을 반환한다`() {
        val connection = UserConnection(
            id = 1L,
            user = testUser,
            partner = partnerUser,
            relationType = RelationType.FRIEND
        )
        whenever(userConnectionRepository.findByUserOrPartnerWithUsers(testUser))
            .thenReturn(listOf(connection))
        whenever(compatibilityRepository.findByConnectionInAndDate(listOf(connection), LocalDate.now()))
            .thenReturn(emptyList())

        val result = connectionService.getMyConnections(testUser)

        assertEquals(1, result.size)
        assertEquals("파트너유저", result[0].partnerName)
        assertEquals(RelationType.FRIEND, result[0].relationType)
    }

    @Test
    fun `getMyConnections - 오늘의 궁합 점수가 있으면 포함한다`() {
        val connection = UserConnection(
            id = 1L,
            user = testUser,
            partner = partnerUser,
            relationType = RelationType.LOVER
        )
        val compatibility = Compatibility(
            id = 1L,
            connection = connection,
            score = 85,
            content = "좋은 궁합",
            date = LocalDate.now()
        )
        whenever(userConnectionRepository.findByUserOrPartnerWithUsers(testUser))
            .thenReturn(listOf(connection))
        whenever(compatibilityRepository.findByConnectionInAndDate(listOf(connection), LocalDate.now()))
            .thenReturn(listOf(compatibility))

        val result = connectionService.getMyConnections(testUser)

        assertEquals(1, result.size)
        assertEquals(85, result[0].latestScore)
    }

    @Test
    fun `getMyConnections - 연결이 없으면 빈 리스트를 반환한다`() {
        whenever(userConnectionRepository.findByUserOrPartnerWithUsers(testUser))
            .thenReturn(emptyList())

        val result = connectionService.getMyConnections(testUser)

        assertEquals(0, result.size)
    }

    @Test
    fun `deleteConnection - 정상적으로 삭제한다`() {
        val connection = UserConnection(
            id = 1L,
            user = testUser,
            partner = partnerUser,
            relationType = RelationType.FRIEND
        )
        whenever(userConnectionRepository.findById(1L)).thenReturn(Optional.of(connection))

        connectionService.deleteConnection(testUser, 1L)

        verify(compatibilityRepository).deleteAllByConnection(connection)
        verify(userConnectionRepository).delete(connection)
    }

    @Test
    fun `deleteConnection - 파트너도 삭제할 수 있다`() {
        val connection = UserConnection(
            id = 1L,
            user = testUser,
            partner = partnerUser,
            relationType = RelationType.FRIEND
        )
        whenever(userConnectionRepository.findById(1L)).thenReturn(Optional.of(connection))

        connectionService.deleteConnection(partnerUser, 1L)

        verify(compatibilityRepository).deleteAllByConnection(connection)
        verify(userConnectionRepository).delete(connection)
    }

    @Test
    fun `deleteConnection - 존재하지 않는 연결이면 ConnectionNotFoundException 발생`() {
        whenever(userConnectionRepository.findById(999L)).thenReturn(Optional.empty())

        assertThrows<ConnectionNotFoundException> {
            connectionService.deleteConnection(testUser, 999L)
        }
    }

    @Test
    fun `deleteConnection - 권한이 없으면 ForbiddenException 발생`() {
        val otherUser = User(
            id = 3L,
            name = "다른유저",
            birthDate = LocalDate.of(1988, 3, 10),
            gender = Gender.MALE,
            calendarType = CalendarType.SOLAR
        )
        val connection = UserConnection(
            id = 1L,
            user = testUser,
            partner = partnerUser,
            relationType = RelationType.FRIEND
        )
        whenever(userConnectionRepository.findById(1L)).thenReturn(Optional.of(connection))

        assertThrows<ForbiddenException> {
            connectionService.deleteConnection(otherUser, 1L)
        }
    }
}
