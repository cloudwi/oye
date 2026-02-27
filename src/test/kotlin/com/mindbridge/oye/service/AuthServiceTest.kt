package com.mindbridge.oye.service

import com.mindbridge.oye.domain.CalendarType
import com.mindbridge.oye.domain.SocialAccount
import com.mindbridge.oye.domain.SocialProvider
import com.mindbridge.oye.domain.User
import com.mindbridge.oye.event.UserCreatedEvent
import com.mindbridge.oye.repository.SocialAccountRepository
import com.mindbridge.oye.repository.UserRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.context.ApplicationEventPublisher
import java.time.LocalDate

@ExtendWith(MockitoExtension::class)
class AuthServiceTest {

    @Mock
    private lateinit var userRepository: UserRepository

    @Mock
    private lateinit var socialAccountRepository: SocialAccountRepository

    @Mock
    private lateinit var eventPublisher: ApplicationEventPublisher

    @InjectMocks
    private lateinit var authService: AuthService

    @Test
    fun `createUser - User와 SocialAccount를 생성하고 이벤트를 발행한다`() {
        val savedUser = User(
            id = 1L,
            name = "테스트유저",
            birthDate = LocalDate.of(2000, 1, 1),
            calendarType = CalendarType.SOLAR
        )
        whenever(userRepository.save(any<User>())).thenReturn(savedUser)
        whenever(socialAccountRepository.save(any<SocialAccount>())).thenAnswer { it.getArgument(0) }

        val result = authService.createUser(SocialProvider.KAKAO, "kakao_123", "테스트유저")

        assertEquals(1L, result.id)
        assertEquals("테스트유저", result.name)

        verify(userRepository).save(argThat<User> {
            name == "테스트유저" &&
                    birthDate == LocalDate.of(2000, 1, 1) &&
                    calendarType == CalendarType.SOLAR
        })

        verify(socialAccountRepository).save(argThat<SocialAccount> {
            user == savedUser &&
                    provider == SocialProvider.KAKAO &&
                    providerId == "kakao_123"
        })

        verify(eventPublisher).publishEvent(UserCreatedEvent(savedUser))
    }

    @Test
    fun `createUser - 이름이 null이면 User의 name이 null이다`() {
        val savedUser = User(
            id = 2L,
            name = null,
            birthDate = LocalDate.of(2000, 1, 1),
            calendarType = CalendarType.SOLAR
        )
        whenever(userRepository.save(any<User>())).thenReturn(savedUser)
        whenever(socialAccountRepository.save(any<SocialAccount>())).thenAnswer { it.getArgument(0) }

        val result = authService.createUser(SocialProvider.APPLE, "apple_456", null)

        assertNull(result.name)
        verify(userRepository).save(argThat<User> { name == null })
    }

    @Test
    fun `createUser - 이름이 빈 문자열이면 User의 name이 null이다`() {
        val savedUser = User(
            id = 3L,
            name = null,
            birthDate = LocalDate.of(2000, 1, 1),
            calendarType = CalendarType.SOLAR
        )
        whenever(userRepository.save(any<User>())).thenReturn(savedUser)
        whenever(socialAccountRepository.save(any<SocialAccount>())).thenAnswer { it.getArgument(0) }

        val result = authService.createUser(SocialProvider.KAKAO, "kakao_789", "")

        assertNull(result.name)
        verify(userRepository).save(argThat<User> { name == null })
    }

    @Test
    fun `createUser - 공백만 있는 이름도 null 처리된다`() {
        val savedUser = User(
            id = 4L,
            name = null,
            birthDate = LocalDate.of(2000, 1, 1),
            calendarType = CalendarType.SOLAR
        )
        whenever(userRepository.save(any<User>())).thenReturn(savedUser)
        whenever(socialAccountRepository.save(any<SocialAccount>())).thenAnswer { it.getArgument(0) }

        val result = authService.createUser(SocialProvider.APPLE, "apple_999", "   ")

        assertNull(result.name)
        verify(userRepository).save(argThat<User> { name == null })
    }

    @Test
    fun `createUser - APPLE provider로도 정상 생성된다`() {
        val savedUser = User(
            id = 5L,
            name = "애플유저",
            birthDate = LocalDate.of(2000, 1, 1),
            calendarType = CalendarType.SOLAR
        )
        whenever(userRepository.save(any<User>())).thenReturn(savedUser)
        whenever(socialAccountRepository.save(any<SocialAccount>())).thenAnswer { it.getArgument(0) }

        val result = authService.createUser(SocialProvider.APPLE, "apple_id_001", "애플유저")

        assertEquals("애플유저", result.name)
        verify(socialAccountRepository).save(argThat<SocialAccount> {
            provider == SocialProvider.APPLE &&
                    providerId == "apple_id_001"
        })
        verify(eventPublisher).publishEvent(UserCreatedEvent(savedUser))
    }
}
