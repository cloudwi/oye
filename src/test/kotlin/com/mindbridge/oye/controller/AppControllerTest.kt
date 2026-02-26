package com.mindbridge.oye.controller

import com.mindbridge.oye.config.TestConfig
import com.mindbridge.oye.domain.AppVersionConfig
import com.mindbridge.oye.repository.AppVersionConfigRepository
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

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestConfig::class)
class AppControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var appVersionConfigRepository: AppVersionConfigRepository

    @BeforeEach
    fun setUp() {
        appVersionConfigRepository.deleteAll()
        appVersionConfigRepository.save(
            AppVersionConfig(
                platform = "ios",
                minVersion = "2.0.0",
                storeUrl = "https://apps.apple.com/app/id000000000"
            )
        )
        appVersionConfigRepository.save(
            AppVersionConfig(
                platform = "android",
                minVersion = "1.5.0",
                storeUrl = "https://play.google.com/store/apps/details?id=com.oyeapp.fortune"
            )
        )
    }

    @Test
    fun `check-update - 버전이 낮으면 forceUpdate true 반환`() {
        mockMvc.perform(
            get("/api/app/check-update")
                .param("platform", "ios")
                .param("version", "1.0.0")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.forceUpdate").value(true))
            .andExpect(jsonPath("$.minVersion").value("2.0.0"))
            .andExpect(jsonPath("$.storeUrl").value("https://apps.apple.com/app/id000000000"))
    }

    @Test
    fun `check-update - 버전이 같으면 forceUpdate false 반환`() {
        mockMvc.perform(
            get("/api/app/check-update")
                .param("platform", "ios")
                .param("version", "2.0.0")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.forceUpdate").value(false))
    }

    @Test
    fun `check-update - 버전이 높으면 forceUpdate false 반환`() {
        mockMvc.perform(
            get("/api/app/check-update")
                .param("platform", "ios")
                .param("version", "3.0.0")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.forceUpdate").value(false))
    }

    @Test
    fun `check-update - android 플랫폼 정상 동작`() {
        mockMvc.perform(
            get("/api/app/check-update")
                .param("platform", "android")
                .param("version", "1.0.0")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.forceUpdate").value(true))
            .andExpect(jsonPath("$.minVersion").value("1.5.0"))
    }

    @Test
    fun `check-update - 마이너 버전 비교 정확성`() {
        mockMvc.perform(
            get("/api/app/check-update")
                .param("platform", "android")
                .param("version", "1.4.9")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.forceUpdate").value(true))

        mockMvc.perform(
            get("/api/app/check-update")
                .param("platform", "android")
                .param("version", "1.5.0")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.forceUpdate").value(false))

        mockMvc.perform(
            get("/api/app/check-update")
                .param("platform", "android")
                .param("version", "1.5.1")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.forceUpdate").value(false))
    }

    @Test
    fun `check-update - 존재하지 않는 플랫폼이면 404 반환`() {
        mockMvc.perform(
            get("/api/app/check-update")
                .param("platform", "windows")
                .param("version", "1.0.0")
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `check-update - 인증 없이 접근 가능 (public 엔드포인트)`() {
        mockMvc.perform(
            get("/api/app/check-update")
                .param("platform", "ios")
                .param("version", "1.0.0")
        )
            .andExpect(status().isOk)
    }

    @Test
    fun `check-update - 대소문자 무관하게 플랫폼 매칭`() {
        mockMvc.perform(
            get("/api/app/check-update")
                .param("platform", "IOS")
                .param("version", "1.0.0")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.forceUpdate").value(true))
    }
}
