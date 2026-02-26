package com.mindbridge.oye.controller

import com.mindbridge.oye.config.JwtTokenProvider
import com.mindbridge.oye.config.TestConfig
import com.mindbridge.oye.domain.CalendarType
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
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDate

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestConfig::class)
class ConnectionControllerTest {

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
                calendarType = CalendarType.LUNAR,
                connectCode = "ABC123"
            )
        )
        accessToken = jwtTokenProvider.generateAccessToken(testUser.id!!)
    }

    @Test
    fun `GET my-code - 내 초대 코드를 반환한다`() {
        mockMvc.perform(
            get("/api/connections/my-code")
                .header("Authorization", "Bearer $accessToken")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").isNotEmpty)
    }

    @Test
    fun `GET my-code - 인증 없이 요청하면 401`() {
        mockMvc.perform(get("/api/connections/my-code"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `POST connections - 연결을 생성한다`() {
        val requestBody = """
            {
                "code": "ABC123",
                "relationType": "FRIEND"
            }
        """.trimIndent()

        mockMvc.perform(
            post("/api/connections")
                .header("Authorization", "Bearer $accessToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.partnerName").value("파트너유저"))
            .andExpect(jsonPath("$.relationType").value("FRIEND"))
    }

    @Test
    fun `POST connections - 존재하지 않는 코드이면 404`() {
        val requestBody = """
            {
                "code": "INVALID",
                "relationType": "FRIEND"
            }
        """.trimIndent()

        mockMvc.perform(
            post("/api/connections")
                .header("Authorization", "Bearer $accessToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `POST connections - 자기 자신과 연결하면 400`() {
        testUser.connectCode = "MYCODE"
        userRepository.save(testUser)

        val requestBody = """
            {
                "code": "MYCODE",
                "relationType": "FRIEND"
            }
        """.trimIndent()

        mockMvc.perform(
            post("/api/connections")
                .header("Authorization", "Bearer $accessToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `POST connections - 이미 연결된 사용자면 409`() {
        userConnectionRepository.save(
            UserConnection(
                user = testUser,
                partner = partnerUser,
                relationType = RelationType.FRIEND
            )
        )

        val requestBody = """
            {
                "code": "ABC123",
                "relationType": "LOVER"
            }
        """.trimIndent()

        mockMvc.perform(
            post("/api/connections")
                .header("Authorization", "Bearer $accessToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andExpect(status().isConflict)
    }

    @Test
    fun `POST connections - 인증 없이 요청하면 401`() {
        val requestBody = """
            {
                "code": "ABC123",
                "relationType": "FRIEND"
            }
        """.trimIndent()

        mockMvc.perform(
            post("/api/connections")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `GET connections - 연결 목록을 반환한다`() {
        userConnectionRepository.save(
            UserConnection(
                user = testUser,
                partner = partnerUser,
                relationType = RelationType.FRIEND
            )
        )

        mockMvc.perform(
            get("/api/connections")
                .header("Authorization", "Bearer $accessToken")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].partnerName").value("파트너유저"))
            .andExpect(jsonPath("$[0].relationType").value("FRIEND"))
    }

    @Test
    fun `GET connections - 연결이 없으면 빈 배열을 반환한다`() {
        mockMvc.perform(
            get("/api/connections")
                .header("Authorization", "Bearer $accessToken")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(0))
    }

    @Test
    fun `GET connections - 인증 없이 요청하면 401`() {
        mockMvc.perform(get("/api/connections"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `DELETE connections - 연결을 삭제한다`() {
        val connection = userConnectionRepository.save(
            UserConnection(
                user = testUser,
                partner = partnerUser,
                relationType = RelationType.FRIEND
            )
        )

        mockMvc.perform(
            delete("/api/connections/${connection.id}")
                .header("Authorization", "Bearer $accessToken")
        )
            .andExpect(status().isOk)

        // 삭제 후 연결이 없는지 확인
        mockMvc.perform(
            get("/api/connections")
                .header("Authorization", "Bearer $accessToken")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(0))
    }

    @Test
    fun `DELETE connections - 존재하지 않는 연결이면 404`() {
        mockMvc.perform(
            delete("/api/connections/999")
                .header("Authorization", "Bearer $accessToken")
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `DELETE connections - 권한이 없으면 403`() {
        val otherUser = userRepository.save(
            User(
                name = "다른유저",
                birthDate = LocalDate.of(1988, 3, 10),
                gender = Gender.MALE,
                calendarType = CalendarType.SOLAR
            )
        )
        val connection = userConnectionRepository.save(
            UserConnection(
                user = otherUser,
                partner = partnerUser,
                relationType = RelationType.FRIEND
            )
        )

        mockMvc.perform(
            delete("/api/connections/${connection.id}")
                .header("Authorization", "Bearer $accessToken")
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `DELETE connections - 인증 없이 요청하면 401`() {
        mockMvc.perform(delete("/api/connections/1"))
            .andExpect(status().isUnauthorized)
    }
}
