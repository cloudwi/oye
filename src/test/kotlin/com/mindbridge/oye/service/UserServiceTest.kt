package com.mindbridge.oye.service

import com.mindbridge.oye.domain.CalendarType
import com.mindbridge.oye.domain.Gender
import com.mindbridge.oye.domain.User
import com.mindbridge.oye.repository.SocialAccountRepository
import com.mindbridge.oye.repository.UserRepository
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import java.time.LocalDate

@ExtendWith(MockitoExtension::class)
class UserServiceTest {

    @Mock
    private lateinit var userRepository: UserRepository

    @Mock
    private lateinit var socialAccountRepository: SocialAccountRepository

    @InjectMocks
    private lateinit var userService: UserService

    private fun createTestUser() = User(
        id = 1L,
        name = "테스트유저",
        birthDate = LocalDate.of(1990, 1, 15),
        gender = Gender.MALE,
        calendarType = CalendarType.SOLAR,
        expoPushToken = "ExponentPushToken[test]"
    )

    @Test
    fun `deleteUser - soft deletes user and removes social accounts for re-registration`() {
        val user = createTestUser()

        userService.deleteUser(user)

        assertNotNull(user.deletedAt)
        assertNull(user.expoPushToken)
        assertNull(user.connectCode)
        verify(socialAccountRepository).deleteAllByUser(user)
        verify(userRepository).save(user)
    }
}
