package com.mindbridge.oye.controller

import com.mindbridge.oye.config.AppleTokenVerifier
import com.mindbridge.oye.config.JwtTokenProvider
import com.mindbridge.oye.config.KakaoTokenVerifier
import com.mindbridge.oye.config.KakaoUserInfo
import com.mindbridge.oye.config.TestConfig
import com.mindbridge.oye.domain.CalendarType
import com.mindbridge.oye.domain.SocialAccount
import com.mindbridge.oye.domain.SocialProvider
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
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDate

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestConfig::class, AuthControllerTest.MockVerifierConfig::class)
class AuthControllerTest {

    @TestConfiguration
    class MockVerifierConfig {
        @Bean
        @Primary
        fun appleTokenVerifier(): AppleTokenVerifier {
            return org.mockito.Mockito.mock(AppleTokenVerifier::class.java)
        }

        @Bean
        @Primary
        fun kakaoTokenVerifier(): KakaoTokenVerifier {
            return org.mockito.Mockito.mock(KakaoTokenVerifier::class.java)
        }
    }

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var socialAccountRepository: SocialAccountRepository

    @Autowired
    private lateinit var inquiryCommentRepository: InquiryCommentRepository

    @Autowired
    private lateinit var inquiryRepository: InquiryRepository

    @Autowired
    private lateinit var compatibilityRepository: CompatibilityRepository

    @Autowired
    private lateinit var userConnectionRepository: UserConnectionRepository

    @Autowired
    private lateinit var lottoRecommendationRepository: LottoRecommendationRepository

    @Autowired
    private lateinit var fortuneRepository: FortuneRepository

    @Autowired
    private lateinit var jwtTokenProvider: JwtTokenProvider

    @Autowired
    private lateinit var appleTokenVerifier: AppleTokenVerifier

    @Autowired
    private lateinit var kakaoTokenVerifier: KakaoTokenVerifier

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
    }

    @Test
    fun `POST auth login apple - 신규 사용자 Apple 로그인 성공`() {
        `when`(appleTokenVerifier.verify("valid-apple-token")).thenReturn("apple-user-123")

        val requestBody = """
            {
                "identityToken": "valid-apple-token",
                "fullName": "홍길동"
            }
        """.trimIndent()

        mockMvc.perform(
            post("/api/auth/login/apple")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.accessToken").isNotEmpty)
            .andExpect(jsonPath("$.refreshToken").isNotEmpty)
            .andExpect(jsonPath("$.isNewUser").value(true))
    }

    @Test
    fun `POST auth login apple - 기존 사용자 Apple 로그인 성공`() {
        val user = userRepository.save(
            User(
                name = "기존유저",
                birthDate = LocalDate.of(2000, 1, 1),
                calendarType = CalendarType.SOLAR
            )
        )
        socialAccountRepository.save(
            SocialAccount(
                user = user,
                provider = SocialProvider.APPLE,
                providerId = "apple-user-123"
            )
        )
        `when`(appleTokenVerifier.verify("valid-apple-token")).thenReturn("apple-user-123")

        val requestBody = """
            {
                "identityToken": "valid-apple-token"
            }
        """.trimIndent()

        mockMvc.perform(
            post("/api/auth/login/apple")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.accessToken").isNotEmpty)
            .andExpect(jsonPath("$.refreshToken").isNotEmpty)
            .andExpect(jsonPath("$.isNewUser").value(false))
    }

    @Test
    fun `POST auth login apple - 유효하지 않은 토큰이면 401`() {
        `when`(appleTokenVerifier.verify("invalid-token")).thenThrow(RuntimeException("invalid"))

        val requestBody = """
            {
                "identityToken": "invalid-token"
            }
        """.trimIndent()

        mockMvc.perform(
            post("/api/auth/login/apple")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `POST auth login kakao native - 신규 사용자 카카오 로그인 성공`() {
        `when`(kakaoTokenVerifier.verify("valid-kakao-token"))
            .thenReturn(KakaoUserInfo(id = "kakao-123", nickname = "카카오유저"))

        val requestBody = """
            {
                "accessToken": "valid-kakao-token"
            }
        """.trimIndent()

        mockMvc.perform(
            post("/api/auth/login/kakao/native")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.accessToken").isNotEmpty)
            .andExpect(jsonPath("$.refreshToken").isNotEmpty)
            .andExpect(jsonPath("$.isNewUser").value(true))
    }

    @Test
    fun `POST auth login kakao native - 기존 사용자 카카오 로그인 성공`() {
        val user = userRepository.save(
            User(
                name = "카카오유저",
                birthDate = LocalDate.of(2000, 1, 1),
                calendarType = CalendarType.SOLAR
            )
        )
        socialAccountRepository.save(
            SocialAccount(
                user = user,
                provider = SocialProvider.KAKAO,
                providerId = "kakao-123"
            )
        )
        `when`(kakaoTokenVerifier.verify("valid-kakao-token"))
            .thenReturn(KakaoUserInfo(id = "kakao-123", nickname = "카카오유저"))

        val requestBody = """
            {
                "accessToken": "valid-kakao-token"
            }
        """.trimIndent()

        mockMvc.perform(
            post("/api/auth/login/kakao/native")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.accessToken").isNotEmpty)
            .andExpect(jsonPath("$.refreshToken").isNotEmpty)
            .andExpect(jsonPath("$.isNewUser").value(false))
    }

    @Test
    fun `POST auth login kakao native - 유효하지 않은 토큰이면 401`() {
        `when`(kakaoTokenVerifier.verify("invalid-token")).thenThrow(RuntimeException("invalid"))

        val requestBody = """
            {
                "accessToken": "invalid-token"
            }
        """.trimIndent()

        mockMvc.perform(
            post("/api/auth/login/kakao/native")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `POST auth refresh - 유효한 리프레시 토큰으로 갱신 성공`() {
        val user = userRepository.save(
            User(
                name = "테스트유저",
                birthDate = LocalDate.of(1990, 1, 15),
                calendarType = CalendarType.SOLAR
            )
        )
        val refreshToken = jwtTokenProvider.generateRefreshToken(user.id!!)

        val requestBody = """
            {
                "refreshToken": "$refreshToken"
            }
        """.trimIndent()

        mockMvc.perform(
            post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.accessToken").isNotEmpty)
            .andExpect(jsonPath("$.refreshToken").isNotEmpty)
    }

    @Test
    fun `POST auth refresh - 유효하지 않은 리프레시 토큰이면 401`() {
        val requestBody = """
            {
                "refreshToken": "invalid-refresh-token"
            }
        """.trimIndent()

        mockMvc.perform(
            post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `POST auth logout - 로그아웃 성공`() {
        mockMvc.perform(post("/api/auth/logout"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.message").value("로그아웃되었습니다."))
    }
}
