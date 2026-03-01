package com.mindbridge.oye.controller

import com.mindbridge.oye.config.JwtTokenProvider
import com.mindbridge.oye.config.TestConfig
import com.mindbridge.oye.domain.CalendarType
import com.mindbridge.oye.domain.Gender
import com.mindbridge.oye.domain.LottoRank
import com.mindbridge.oye.domain.LottoRecommendation
import com.mindbridge.oye.domain.LottoRound
import com.mindbridge.oye.domain.User
import com.mindbridge.oye.repository.CompatibilityRepository
import com.mindbridge.oye.repository.FortuneRepository
import com.mindbridge.oye.repository.InquiryCommentRepository
import com.mindbridge.oye.repository.InquiryRepository
import com.mindbridge.oye.repository.LottoRecommendationRepository
import com.mindbridge.oye.repository.LottoRoundRepository
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDate

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestConfig::class)
class LottoControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var lottoRecommendationRepository: LottoRecommendationRepository

    @Autowired
    private lateinit var lottoRoundRepository: LottoRoundRepository

    @Autowired
    private lateinit var fortuneRepository: FortuneRepository

    @Autowired
    private lateinit var socialAccountRepository: SocialAccountRepository

    @Autowired
    private lateinit var compatibilityRepository: CompatibilityRepository

    @Autowired
    private lateinit var userConnectionRepository: UserConnectionRepository

    @Autowired
    private lateinit var inquiryCommentRepository: InquiryCommentRepository

    @Autowired
    private lateinit var inquiryRepository: InquiryRepository

    @Autowired
    private lateinit var jwtTokenProvider: JwtTokenProvider

    private lateinit var testUser: User
    private lateinit var accessToken: String

    @BeforeEach
    fun setUp() {
        inquiryCommentRepository.deleteAll()
        inquiryRepository.deleteAll()
        compatibilityRepository.deleteAll()
        userConnectionRepository.deleteAll()
        lottoRecommendationRepository.deleteAll()
        lottoRoundRepository.deleteAll()
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
        accessToken = jwtTokenProvider.generateAccessToken(testUser.id!!)
    }

    @Test
    fun `POST recommendations - 추천 번호 5세트를 생성한다`() {
        mockMvc.perform(
            post("/api/v1/lotto/recommendations")
                .header("Authorization", "Bearer $accessToken")
                .param("round", "1130")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.length()").value(5))
            .andExpect(jsonPath("$.data[0].round").value(1130))
            .andExpect(jsonPath("$.data[0].setNumber").value(1))
            .andExpect(jsonPath("$.data[0].numbers.length()").value(6))
    }

    @Test
    fun `POST recommendations - 이미 추천받은 회차이면 409`() {
        lottoRecommendationRepository.save(
            LottoRecommendation(
                user = testUser, round = 1130, setNumber = 1,
                number1 = 1, number2 = 10, number3 = 20, number4 = 30, number5 = 40, number6 = 45
            )
        )

        mockMvc.perform(
            post("/api/v1/lotto/recommendations")
                .header("Authorization", "Bearer $accessToken")
                .param("round", "1130")
        )
            .andExpect(status().isConflict)
    }

    @Test
    fun `POST recommendations - 인증 없이 요청하면 401`() {
        mockMvc.perform(
            post("/api/v1/lotto/recommendations")
                .param("round", "1130")
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `GET recommendations - 히스토리를 반환한다`() {
        (1..3).forEach { i ->
            lottoRecommendationRepository.save(
                LottoRecommendation(
                    user = testUser, round = 1130, setNumber = i,
                    number1 = 1, number2 = 10, number3 = 20, number4 = 30, number5 = 40, number6 = 45
                )
            )
        }

        mockMvc.perform(
            get("/api/v1/lotto/recommendations")
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
    fun `GET recommendations - 기본 페이지네이션 파라미터 사용`() {
        mockMvc.perform(
            get("/api/v1/lotto/recommendations")
                .header("Authorization", "Bearer $accessToken")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.content").isArray)
            .andExpect(jsonPath("$.data.page").value(0))
            .andExpect(jsonPath("$.data.size").value(20))
    }

    @Test
    fun `GET recommendations - 인증 없이 요청하면 401`() {
        mockMvc.perform(get("/api/v1/lotto/recommendations"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `GET winners - 당첨자 목록을 반환한다`() {
        val lottoRound = lottoRoundRepository.save(
            LottoRound(
                round = 1130,
                number1 = 3, number2 = 11, number3 = 19, number4 = 25, number5 = 33, number6 = 42,
                bonusNumber = 7, drawDate = LocalDate.of(2025, 6, 14)
            )
        )
        lottoRecommendationRepository.save(
            LottoRecommendation(
                user = testUser, round = 1130, setNumber = 1,
                number1 = 3, number2 = 11, number3 = 19, number4 = 25, number5 = 33, number6 = 42,
                rank = LottoRank.FIRST, matchCount = 6, bonusMatch = false
            )
        )

        mockMvc.perform(
            get("/api/v1/lotto/winners")
                .header("Authorization", "Bearer $accessToken")
                .param("page", "0")
                .param("size", "20")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.content.length()").value(1))
            .andExpect(jsonPath("$.data.content[0].rank").value("1등"))
            .andExpect(jsonPath("$.data.content[0].matchCount").value(6))
            .andExpect(jsonPath("$.data.content[0].drawDate").value("2025-06-14"))
    }

    @Test
    fun `GET winners - 당첨자가 없으면 빈 목록 반환`() {
        mockMvc.perform(
            get("/api/v1/lotto/winners")
                .header("Authorization", "Bearer $accessToken")
                .param("page", "0")
                .param("size", "20")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.content.length()").value(0))
    }

    @Test
    fun `GET rounds - 회차 당첨 번호를 반환한다`() {
        lottoRoundRepository.save(
            LottoRound(
                round = 1130,
                number1 = 3, number2 = 11, number3 = 19, number4 = 25, number5 = 33, number6 = 42,
                bonusNumber = 7, drawDate = LocalDate.of(2025, 6, 14)
            )
        )

        mockMvc.perform(
            get("/api/v1/lotto/rounds/1130")
                .header("Authorization", "Bearer $accessToken")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.round").value(1130))
            .andExpect(jsonPath("$.data.numbers.length()").value(6))
            .andExpect(jsonPath("$.data.bonusNumber").value(7))
            .andExpect(jsonPath("$.data.drawDate").value("2025-06-14"))
    }

    @Test
    fun `GET rounds - 존재하지 않는 회차이면 404`() {
        mockMvc.perform(
            get("/api/v1/lotto/rounds/9999")
                .header("Authorization", "Bearer $accessToken")
        )
            .andExpect(status().isNotFound)
    }
}
