package com.mindbridge.oye.service

import com.mindbridge.oye.domain.CalendarType
import com.mindbridge.oye.domain.Gender
import com.mindbridge.oye.domain.User
import com.mindbridge.oye.repository.FortuneRepository
import com.mindbridge.oye.repository.InquiryRepository
import com.mindbridge.oye.repository.SocialAccountRepository
import com.mindbridge.oye.repository.UserRepository
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InOrder
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.inOrder
import org.mockito.junit.jupiter.MockitoExtension
import java.time.LocalDate

@ExtendWith(MockitoExtension::class)
class UserServiceTest {

    @Mock
    private lateinit var userRepository: UserRepository

    @Mock
    private lateinit var fortuneRepository: FortuneRepository

    @Mock
    private lateinit var socialAccountRepository: SocialAccountRepository

    @Mock
    private lateinit var inquiryRepository: InquiryRepository

    @InjectMocks
    private lateinit var userService: UserService

    private val testUser = User(
        id = 1L,
        name = "테스트유저",
        birthDate = LocalDate.of(1990, 1, 15),
        gender = Gender.MALE,
        calendarType = CalendarType.SOLAR
    )

    @Test
    fun `deleteUser - deletes fortunes and social accounts before deleting user`() {
        userService.deleteUser(testUser)

        val inOrder: InOrder = inOrder(inquiryRepository, fortuneRepository, socialAccountRepository, userRepository)
        inOrder.verify(inquiryRepository).deleteAllByUser(testUser)
        inOrder.verify(fortuneRepository).deleteAllByUser(testUser)
        inOrder.verify(socialAccountRepository).deleteAllByUser(testUser)
        inOrder.verify(userRepository).delete(testUser)
    }
}
