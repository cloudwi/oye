package com.mindbridge.oye.repository

import com.mindbridge.oye.domain.CalendarType
import com.mindbridge.oye.domain.Fortune
import com.mindbridge.oye.domain.Gender
import com.mindbridge.oye.domain.User
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.data.domain.PageRequest
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDate

@DataJpaTest
@ActiveProfiles("test")
class FortuneRepositoryTest {

    @Autowired
    private lateinit var fortuneRepository: FortuneRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    private lateinit var testUser: User

    @BeforeEach
    fun setUp() {
        fortuneRepository.deleteAll()
        userRepository.deleteAll()
        testUser = userRepository.save(
            User(
                kakaoId = "kakao789",
                name = "리포지토리테스트",
                birthDate = LocalDate.of(1985, 7, 10),
                gender = Gender.FEMALE,
                calendarType = CalendarType.SOLAR
            )
        )
    }

    @Test
    fun `findByUserAndDate - returns fortune for matching user and date`() {
        val today = LocalDate.now()
        val fortune = fortuneRepository.save(
            Fortune(user = testUser, content = "오늘의 예감", date = today)
        )

        val result = fortuneRepository.findByUserAndDate(testUser, today)

        assertNotNull(result)
        assertEquals(fortune.id, result!!.id)
        assertEquals("오늘의 예감", result.content)
    }

    @Test
    fun `findByUserAndDate - returns null when no fortune for date`() {
        fortuneRepository.save(
            Fortune(user = testUser, content = "어제의 예감", date = LocalDate.now().minusDays(1))
        )

        val result = fortuneRepository.findByUserAndDate(testUser, LocalDate.now())

        assertNull(result)
    }

    @Test
    fun `findByUserAndDate - returns null for different user`() {
        val otherUser = userRepository.save(
            User(
                kakaoId = "kakao999",
                name = "다른유저",
                birthDate = LocalDate.of(2000, 1, 1)
            )
        )
        fortuneRepository.save(
            Fortune(user = otherUser, content = "다른유저의 예감", date = LocalDate.now())
        )

        val result = fortuneRepository.findByUserAndDate(testUser, LocalDate.now())

        assertNull(result)
    }

    @Test
    fun `findByUserOrderByDateDesc with pageable - returns paginated results`() {
        (1..5).forEach { i ->
            fortuneRepository.save(
                Fortune(
                    user = testUser,
                    content = "예감 $i",
                    date = LocalDate.now().minusDays(i.toLong())
                )
            )
        }

        val page = fortuneRepository.findByUserOrderByDateDesc(testUser, PageRequest.of(0, 3))

        assertEquals(3, page.content.size)
        assertEquals(5, page.totalElements)
        assertEquals(2, page.totalPages)
        assertTrue(page.content[0].date >= page.content[1].date)
    }

    @Test
    fun `findByUserOrderByDateDesc with pageable - returns second page`() {
        (1..5).forEach { i ->
            fortuneRepository.save(
                Fortune(
                    user = testUser,
                    content = "예감 $i",
                    date = LocalDate.now().minusDays(i.toLong())
                )
            )
        }

        val page = fortuneRepository.findByUserOrderByDateDesc(testUser, PageRequest.of(1, 3))

        assertEquals(2, page.content.size)
        assertEquals(5, page.totalElements)
    }

    @Test
    fun `findByUserOrderByDateDesc with pageable - returns empty for user with no fortunes`() {
        val otherUser = userRepository.save(
            User(
                kakaoId = "kakao000",
                name = "빈유저",
                birthDate = LocalDate.of(2000, 1, 1)
            )
        )

        val page = fortuneRepository.findByUserOrderByDateDesc(otherUser, PageRequest.of(0, 20))

        assertEquals(0, page.content.size)
        assertEquals(0, page.totalElements)
    }

    @Test
    fun `deleteAllByUser - removes all fortunes for user`() {
        (1..3).forEach { i ->
            fortuneRepository.save(
                Fortune(
                    user = testUser,
                    content = "삭제할 예감 $i",
                    date = LocalDate.now().minusDays(i.toLong())
                )
            )
        }
        val otherUser = userRepository.save(
            User(
                kakaoId = "kakao111",
                name = "보존유저",
                birthDate = LocalDate.of(2000, 1, 1)
            )
        )
        fortuneRepository.save(
            Fortune(user = otherUser, content = "보존될 예감", date = LocalDate.now())
        )

        fortuneRepository.deleteAllByUser(testUser)

        val testUserFortunes = fortuneRepository.findByUserOrderByDateDesc(testUser)
        val otherUserFortunes = fortuneRepository.findByUserOrderByDateDesc(otherUser)

        assertTrue(testUserFortunes.isEmpty())
        assertEquals(1, otherUserFortunes.size)
    }
}
