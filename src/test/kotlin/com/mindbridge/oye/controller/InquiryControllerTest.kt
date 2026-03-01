package com.mindbridge.oye.controller

import com.mindbridge.oye.config.JwtTokenProvider
import com.mindbridge.oye.config.TestConfig
import com.mindbridge.oye.domain.CalendarType
import com.mindbridge.oye.domain.Gender
import com.mindbridge.oye.domain.Inquiry
import com.mindbridge.oye.domain.User
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDate

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestConfig::class)
class InquiryControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var inquiryRepository: InquiryRepository

    @Autowired
    private lateinit var inquiryCommentRepository: InquiryCommentRepository

    @Autowired
    private lateinit var compatibilityRepository: CompatibilityRepository

    @Autowired
    private lateinit var userConnectionRepository: UserConnectionRepository

    @Autowired
    private lateinit var lottoRecommendationRepository: LottoRecommendationRepository

    @Autowired
    private lateinit var fortuneRepository: FortuneRepository

    @Autowired
    private lateinit var socialAccountRepository: SocialAccountRepository

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
        fortuneRepository.deleteAll()
        socialAccountRepository.deleteAll()
        userRepository.deleteAll()
        testUser = userRepository.save(
            User(
                name = "문의유저",
                birthDate = LocalDate.of(1990, 1, 15),
                gender = Gender.MALE,
                calendarType = CalendarType.SOLAR
            )
        )
        accessToken = jwtTokenProvider.generateAccessToken(testUser.id!!)
    }

    @Test
    fun `POST inquiries - 문의 작성 성공`() {
        val requestBody = """
            {
                "title": "앱 오류 문의",
                "content": "앱이 자꾸 종료됩니다"
            }
        """.trimIndent()

        mockMvc.perform(
            post("/api/v1/inquiries")
                .header("Authorization", "Bearer $accessToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.title").value("앱 오류 문의"))
            .andExpect(jsonPath("$.content").value("앱이 자꾸 종료됩니다"))
            .andExpect(jsonPath("$.status").value("PENDING"))
    }

    @Test
    fun `POST inquiries - 인증 없이 문의 작성 시 401`() {
        val requestBody = """
            {
                "title": "앱 오류 문의",
                "content": "앱이 자꾸 종료됩니다"
            }
        """.trimIndent()

        mockMvc.perform(
            post("/api/v1/inquiries")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `POST inquiries - 제목 빈 값이면 400`() {
        val requestBody = """
            {
                "title": "",
                "content": "내용입니다"
            }
        """.trimIndent()

        mockMvc.perform(
            post("/api/v1/inquiries")
                .header("Authorization", "Bearer $accessToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `POST inquiries - 내용 빈 값이면 400`() {
        val requestBody = """
            {
                "title": "제목입니다",
                "content": ""
            }
        """.trimIndent()

        mockMvc.perform(
            post("/api/v1/inquiries")
                .header("Authorization", "Bearer $accessToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `GET inquiries - 내 문의 목록 조회 성공`() {
        (1..3).forEach { i ->
            inquiryRepository.save(
                Inquiry(
                    user = testUser,
                    title = "문의 $i",
                    content = "내용 $i"
                )
            )
        }

        mockMvc.perform(
            get("/api/v1/inquiries")
                .header("Authorization", "Bearer $accessToken")
                .param("page", "0")
                .param("size", "20")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.content.length()").value(3))
            .andExpect(jsonPath("$.data.page").value(0))
            .andExpect(jsonPath("$.data.size").value(20))
            .andExpect(jsonPath("$.data.totalElements").value(3))
    }

    @Test
    fun `GET inquiries - 인증 없이 목록 조회 시 401`() {
        mockMvc.perform(get("/api/v1/inquiries"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `GET inquiries id - 문의 상세 조회 성공`() {
        val inquiry = inquiryRepository.save(
            Inquiry(
                user = testUser,
                title = "상세 문의",
                content = "상세 내용"
            )
        )

        mockMvc.perform(
            get("/api/v1/inquiries/${inquiry.id}")
                .header("Authorization", "Bearer $accessToken")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(inquiry.id))
            .andExpect(jsonPath("$.title").value("상세 문의"))
            .andExpect(jsonPath("$.content").value("상세 내용"))
            .andExpect(jsonPath("$.status").value("PENDING"))
            .andExpect(jsonPath("$.comments").isArray)
    }

    @Test
    fun `GET inquiries id - 타인 문의 조회 시 403`() {
        val otherUser = userRepository.save(
            User(
                name = "다른유저",
                birthDate = LocalDate.of(1992, 3, 20),
                gender = Gender.FEMALE,
                calendarType = CalendarType.SOLAR
            )
        )
        val inquiry = inquiryRepository.save(
            Inquiry(
                user = otherUser,
                title = "다른유저 문의",
                content = "다른유저 문의 내용"
            )
        )

        mockMvc.perform(
            get("/api/v1/inquiries/${inquiry.id}")
                .header("Authorization", "Bearer $accessToken")
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `GET inquiries id - 존재하지 않는 문의 조회 시 404`() {
        mockMvc.perform(
            get("/api/v1/inquiries/999999")
                .header("Authorization", "Bearer $accessToken")
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `GET inquiries id - 인증 없이 상세 조회 시 401`() {
        mockMvc.perform(get("/api/v1/inquiries/1"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `POST inquiries id comments - 관리자가 아닌 사용자가 댓글 작성 시 403`() {
        val inquiry = inquiryRepository.save(
            Inquiry(
                user = testUser,
                title = "테스트 문의",
                content = "내용"
            )
        )

        val requestBody = """
            {
                "content": "답변 내용"
            }
        """.trimIndent()

        mockMvc.perform(
            post("/api/v1/inquiries/${inquiry.id}/comments")
                .header("Authorization", "Bearer $accessToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `POST inquiries id comments - 인증 없이 댓글 작성 시 401`() {
        val requestBody = """
            {
                "content": "답변 내용"
            }
        """.trimIndent()

        mockMvc.perform(
            post("/api/v1/inquiries/1/comments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andExpect(status().isUnauthorized)
    }
}
