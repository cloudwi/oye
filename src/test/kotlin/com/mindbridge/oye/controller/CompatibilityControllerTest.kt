package com.mindbridge.oye.controller

import com.mindbridge.oye.config.JwtTokenProvider
import com.mindbridge.oye.config.TestConfig
import com.mindbridge.oye.domain.CalendarType
import com.mindbridge.oye.domain.Compatibility
import com.mindbridge.oye.domain.Gender
import com.mindbridge.oye.domain.RelationType
import com.mindbridge.oye.domain.User
import com.mindbridge.oye.domain.UserConnection
import com.mindbridge.oye.repository.CompatibilityRepository
import com.mindbridge.oye.repository.FortuneRepository
import com.mindbridge.oye.repository.InquiryCommentRepository
import com.mindbridge.oye.repository.InquiryRepository
import com.mindbridge.oye.repository.LottoRecommendationRepository
import com.mindbridge.oye.repository.SocialAccountRepository
import com.mindbridge.oye.repository.UserConnectionRepository
import com.mindbridge.oye.repository.UserRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
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
class CompatibilityControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var userConnectionRepository: UserConnectionRepository

    @Autowired
    private lateinit var compatibilityRepository: CompatibilityRepository

    @Autowired
    private lateinit var fortuneRepository: FortuneRepository

    @Autowired
    private lateinit var socialAccountRepository: SocialAccountRepository

    @Autowired
    private lateinit var lottoRecommendationRepository: LottoRecommendationRepository

    @Autowired
    private lateinit var inquiryCommentRepository: InquiryCommentRepository

    @Autowired
    private lateinit var inquiryRepository: InquiryRepository

    @Autowired
    private lateinit var jwtTokenProvider: JwtTokenProvider

    private lateinit var testUser: User
    private lateinit var partnerUser: User
    private lateinit var connection: UserConnection
    private lateinit var accessToken: String

    @BeforeEach
    fun setUp() {
        inquiryCommentRepository.deleteAll()
        inquiryRepository.deleteAll()
        compatibilityRepository.deleteAll()
        userConnectionRepository.deleteAll()
        lottoRecommendationRepository.deleteAll()
        fortuneRepository.deleteAll()
        socialAccountRepository.deleteAll()
        userRepository.deleteAll()

        testUser = userRepository.save(
            User(
                name = "테스트유저",
                birthDate = LocalDate.of(1990, 1, 15),
                gender = Gender.MALE,
                calendarType = CalendarType.SOLAR
            )
        )
        partnerUser = userRepository.save(
            User(
                name = "파트너유저",
                birthDate = LocalDate.of(1992, 5, 20),
                gender = Gender.FEMALE,
                calendarType = CalendarType.LUNAR
            )
        )
        connection = userConnectionRepository.save(
            UserConnection(
                user = testUser,
                partner = partnerUser,
                relationType = RelationType.LOVER
            )
        )
        accessToken = jwtTokenProvider.generateAccessToken(testUser.id!!)
    }

    @Test
    fun `GET compatibility - 기존 궁합이 있으면 반환한다`() {
        compatibilityRepository.save(
            Compatibility(
                connection = connection,
                score = 85,
                content = "오늘 두 분의 궁합은 아주 좋아요.",
                date = LocalDate.now()
            )
        )

        mockMvc.perform(
            get("/api/v1/connections/${connection.id}/compatibility")
                .header("Authorization", "Bearer $accessToken")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.score").value(85))
            .andExpect(jsonPath("$.data.content").value("오늘 두 분의 궁합은 아주 좋아요."))
    }

    @Test
    fun `GET compatibility - 존재하지 않는 연결이면 404`() {
        mockMvc.perform(
            get("/api/v1/connections/999/compatibility")
                .header("Authorization", "Bearer $accessToken")
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `GET compatibility - 권한이 없으면 403`() {
        val otherUser = userRepository.save(
            User(
                name = "다른유저",
                birthDate = LocalDate.of(1988, 3, 10),
                gender = Gender.MALE,
                calendarType = CalendarType.SOLAR
            )
        )
        val otherToken = jwtTokenProvider.generateAccessToken(otherUser.id!!)

        mockMvc.perform(
            get("/api/v1/connections/${connection.id}/compatibility")
                .header("Authorization", "Bearer $otherToken")
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `GET compatibility - 인증 없이 요청하면 401`() {
        mockMvc.perform(get("/api/v1/connections/${connection.id}/compatibility"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `GET compatibility history - 히스토리를 반환한다`() {
        (1..3).forEach { i ->
            compatibilityRepository.save(
                Compatibility(
                    connection = connection,
                    score = 80 + i,
                    content = "궁합 내용 $i",
                    date = LocalDate.now().minusDays(i.toLong())
                )
            )
        }

        mockMvc.perform(
            get("/api/v1/connections/${connection.id}/compatibility/history")
                .header("Authorization", "Bearer $accessToken")
                .param("page", "0")
                .param("size", "2")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.content.length()").value(2))
            .andExpect(jsonPath("$.data.page").value(0))
            .andExpect(jsonPath("$.data.size").value(2))
            .andExpect(jsonPath("$.data.totalElements").value(3))
            .andExpect(jsonPath("$.data.totalPages").value(2))
    }

    @Test
    fun `GET compatibility history - 기본 페이지네이션 파라미터 사용`() {
        mockMvc.perform(
            get("/api/v1/connections/${connection.id}/compatibility/history")
                .header("Authorization", "Bearer $accessToken")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.content").isArray)
            .andExpect(jsonPath("$.data.page").value(0))
            .andExpect(jsonPath("$.data.size").value(20))
    }

    @Test
    fun `GET compatibility history - 존재하지 않는 연결이면 404`() {
        mockMvc.perform(
            get("/api/v1/connections/999/compatibility/history")
                .header("Authorization", "Bearer $accessToken")
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `GET compatibility history - 권한이 없으면 403`() {
        val otherUser = userRepository.save(
            User(
                name = "다른유저",
                birthDate = LocalDate.of(1988, 3, 10),
                gender = Gender.MALE,
                calendarType = CalendarType.SOLAR
            )
        )
        val otherToken = jwtTokenProvider.generateAccessToken(otherUser.id!!)

        mockMvc.perform(
            get("/api/v1/connections/${connection.id}/compatibility/history")
                .header("Authorization", "Bearer $otherToken")
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `GET compatibility history - 인증 없이 요청하면 401`() {
        mockMvc.perform(get("/api/v1/connections/${connection.id}/compatibility/history"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `GET score-trend - 점수 추이를 반환한다`() {
        (1..3).forEach { i ->
            compatibilityRepository.save(
                Compatibility(
                    connection = connection,
                    score = 70 + i * 5,
                    content = "궁합 내용 $i",
                    date = LocalDate.now().minusDays(i.toLong())
                )
            )
        }

        mockMvc.perform(
            get("/api/v1/connections/${connection.id}/compatibility/score-trend")
                .header("Authorization", "Bearer $accessToken")
                .param("days", "7")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.length()").value(3))
            .andExpect(jsonPath("$.data[0].date").exists())
            .andExpect(jsonPath("$.data[0].score").exists())
    }

    @Test
    fun `GET score-trend - 기본 days 파라미터 사용`() {
        mockMvc.perform(
            get("/api/v1/connections/${connection.id}/compatibility/score-trend")
                .header("Authorization", "Bearer $accessToken")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").isArray)
    }

    @Test
    fun `GET score-trend - 존재하지 않는 연결이면 404`() {
        mockMvc.perform(
            get("/api/v1/connections/999/compatibility/score-trend")
                .header("Authorization", "Bearer $accessToken")
                .param("days", "7")
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `GET score-trend - 권한이 없으면 403`() {
        val otherUser = userRepository.save(
            User(
                name = "다른유저",
                birthDate = LocalDate.of(1988, 3, 10),
                gender = Gender.MALE,
                calendarType = CalendarType.SOLAR
            )
        )
        val otherToken = jwtTokenProvider.generateAccessToken(otherUser.id!!)

        mockMvc.perform(
            get("/api/v1/connections/${connection.id}/compatibility/score-trend")
                .header("Authorization", "Bearer $otherToken")
                .param("days", "7")
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `GET score-trend - 인증 없이 요청하면 401`() {
        mockMvc.perform(get("/api/v1/connections/${connection.id}/compatibility/score-trend"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `GET record-dates - 기록 날짜를 반환한다`() {
        val targetDate = LocalDate.of(2026, 3, 10)
        compatibilityRepository.save(
            Compatibility(
                connection = connection,
                score = 85,
                content = "궁합 내용",
                date = targetDate
            )
        )

        mockMvc.perform(
            get("/api/v1/connections/${connection.id}/compatibility/record-dates")
                .header("Authorization", "Bearer $accessToken")
                .param("year", "2026")
                .param("month", "3")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.yearMonth").value("2026-03"))
            .andExpect(jsonPath("$.data.dates.length()").value(1))
            .andExpect(jsonPath("$.data.dates[0]").value("2026-03-10"))
    }

    @Test
    fun `GET record-dates - 존재하지 않는 연결이면 404`() {
        mockMvc.perform(
            get("/api/v1/connections/999/compatibility/record-dates")
                .header("Authorization", "Bearer $accessToken")
                .param("year", "2026")
                .param("month", "3")
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `GET record-dates - 권한이 없으면 403`() {
        val otherUser = userRepository.save(
            User(
                name = "다른유저2",
                birthDate = LocalDate.of(1988, 3, 10),
                gender = Gender.MALE,
                calendarType = CalendarType.SOLAR
            )
        )
        val otherToken = jwtTokenProvider.generateAccessToken(otherUser.id!!)

        mockMvc.perform(
            get("/api/v1/connections/${connection.id}/compatibility/record-dates")
                .header("Authorization", "Bearer $otherToken")
                .param("year", "2026")
                .param("month", "3")
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `GET record-dates - 인증 없이 요청하면 401`() {
        mockMvc.perform(
            get("/api/v1/connections/${connection.id}/compatibility/record-dates")
                .param("year", "2026")
                .param("month", "3")
        )
            .andExpect(status().isUnauthorized)
    }
}
