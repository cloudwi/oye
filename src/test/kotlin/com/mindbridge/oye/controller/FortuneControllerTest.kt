package com.mindbridge.oye.controller

import com.mindbridge.oye.config.JwtTokenProvider
import com.mindbridge.oye.config.TestConfig
import com.mindbridge.oye.domain.CalendarType
import com.mindbridge.oye.domain.Fortune
import com.mindbridge.oye.domain.Gender
import com.mindbridge.oye.domain.User
import com.mindbridge.oye.repository.FortuneRepository
import com.mindbridge.oye.repository.UserRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDate

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestConfig::class)
class FortuneControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var fortuneRepository: FortuneRepository

    @Autowired
    private lateinit var jwtTokenProvider: JwtTokenProvider

    private lateinit var testUser: User
    private lateinit var accessToken: String

    @BeforeEach
    fun setUp() {
        fortuneRepository.deleteAll()
        userRepository.deleteAll()
        testUser = userRepository.save(
            User(
                name = "예감유저",
                birthDate = LocalDate.of(1995, 3, 20),
                gender = Gender.FEMALE,
                calendarType = CalendarType.LUNAR
            )
        )
        accessToken = jwtTokenProvider.generateAccessToken(testUser.id!!)
    }

    @Test
    fun `GET fortune today - returns fortune for authenticated user`() {
        val fortune = fortuneRepository.save(
            Fortune(
                user = testUser,
                content = "오늘은 좋은 일이 생길 수 있는 날이에요.",
                date = LocalDate.now()
            )
        )

        mockMvc.perform(
            get("/api/fortune/today")
                .header("Authorization", "Bearer $accessToken")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(fortune.id))
            .andExpect(jsonPath("$.content").value("오늘은 좋은 일이 생길 수 있는 날이에요."))
            .andExpect(jsonPath("$.date").value(LocalDate.now().toString()))
    }

    @Test
    fun `GET fortune today - returns 401 without token`() {
        mockMvc.perform(get("/api/fortune/today"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `GET fortune history - returns paginated history`() {
        val fortunes = (1..5).map { i ->
            fortuneRepository.save(
                Fortune(
                    user = testUser,
                    content = "예감 $i",
                    date = LocalDate.now().minusDays(i.toLong())
                )
            )
        }

        mockMvc.perform(
            get("/api/fortune/history")
                .header("Authorization", "Bearer $accessToken")
                .param("page", "0")
                .param("size", "3")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.content.length()").value(3))
            .andExpect(jsonPath("$.data.page").value(0))
            .andExpect(jsonPath("$.data.size").value(3))
            .andExpect(jsonPath("$.data.totalElements").value(5))
            .andExpect(jsonPath("$.data.totalPages").value(2))
    }

    @Test
    fun `GET fortune history - uses default pagination params`() {
        mockMvc.perform(
            get("/api/fortune/history")
                .header("Authorization", "Bearer $accessToken")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.content").isArray)
            .andExpect(jsonPath("$.data.page").value(0))
            .andExpect(jsonPath("$.data.size").value(20))
    }

    @Test
    fun `GET fortune history - returns 401 without token`() {
        mockMvc.perform(get("/api/fortune/history"))
            .andExpect(status().isUnauthorized)
    }
}
