package com.mindbridge.oye.controller

import com.mindbridge.oye.config.JwtTokenProvider
import com.mindbridge.oye.config.TestConfig
import com.mindbridge.oye.domain.CalendarType
import com.mindbridge.oye.domain.Gender
import com.mindbridge.oye.domain.User
import com.mindbridge.oye.repository.FortuneRepository
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDate

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestConfig::class)
class UserControllerTest {

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
                name = "테스트유저",
                birthDate = LocalDate.of(1990, 1, 15),
                gender = Gender.MALE,
                calendarType = CalendarType.SOLAR
            )
        )
        accessToken = jwtTokenProvider.generateAccessToken(testUser.id!!)
    }

    @Test
    fun `GET users me - returns user profile`() {
        mockMvc.perform(
            get("/api/users/me")
                .header("Authorization", "Bearer $accessToken")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(testUser.id))
            .andExpect(jsonPath("$.name").value("테스트유저"))
            .andExpect(jsonPath("$.birthDate").value("1990-01-15"))
            .andExpect(jsonPath("$.gender").value("MALE"))
            .andExpect(jsonPath("$.calendarType").value("SOLAR"))
    }

    @Test
    fun `GET users me - returns 401 without token`() {
        mockMvc.perform(get("/api/users/me"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `PUT users me - updates user profile`() {
        val requestBody = """
            {
                "name": "수정된이름",
                "birthDate": "1991-06-20",
                "gender": "FEMALE",
                "calendarType": "LUNAR"
            }
        """.trimIndent()

        mockMvc.perform(
            put("/api/users/me")
                .header("Authorization", "Bearer $accessToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("수정된이름"))
            .andExpect(jsonPath("$.birthDate").value("1991-06-20"))
            .andExpect(jsonPath("$.gender").value("FEMALE"))
            .andExpect(jsonPath("$.calendarType").value("LUNAR"))
    }

    @Test
    fun `PUT users me - returns 400 when name is blank`() {
        val requestBody = """
            {
                "name": "",
                "birthDate": "1990-01-15"
            }
        """.trimIndent()

        mockMvc.perform(
            put("/api/users/me")
                .header("Authorization", "Bearer $accessToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `PUT users me - returns 400 when birthDate is in the future`() {
        val futureDate = LocalDate.now().plusYears(1).toString()
        val requestBody = """
            {
                "name": "테스트유저",
                "birthDate": "$futureDate"
            }
        """.trimIndent()

        mockMvc.perform(
            put("/api/users/me")
                .header("Authorization", "Bearer $accessToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `PUT users me - returns 401 without token`() {
        val requestBody = """
            {
                "name": "수정된이름",
                "birthDate": "1991-06-20"
            }
        """.trimIndent()

        mockMvc.perform(
            put("/api/users/me")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `DELETE users me - deletes account successfully`() {
        mockMvc.perform(
            delete("/api/users/me")
                .header("Authorization", "Bearer $accessToken")
        )
            .andExpect(status().isNoContent)

        // Verify user is deleted
        mockMvc.perform(
            get("/api/users/me")
                .header("Authorization", "Bearer $accessToken")
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `DELETE users me - returns 401 without token`() {
        mockMvc.perform(delete("/api/users/me"))
            .andExpect(status().isUnauthorized)
    }
}
