package com.mindbridge.oye.service

import com.mindbridge.oye.domain.AppVersionConfig
import com.mindbridge.oye.domain.CalendarType
import com.mindbridge.oye.domain.Gender
import com.mindbridge.oye.domain.Role
import com.mindbridge.oye.domain.User
import com.mindbridge.oye.dto.AppVersionUpdateRequest
import com.mindbridge.oye.exception.ForbiddenException
import com.mindbridge.oye.repository.AppVersionConfigRepository
import com.mindbridge.oye.repository.InquiryRepository
import com.mindbridge.oye.repository.UserRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import java.time.LocalDate
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
class AdminServiceTest {

    @Mock
    private lateinit var userRepository: UserRepository

    @Mock
    private lateinit var inquiryRepository: InquiryRepository

    @Mock
    private lateinit var appVersionConfigRepository: AppVersionConfigRepository

    @InjectMocks
    private lateinit var adminService: AdminService

    private val adminUser = User(
        id = 1L,
        name = "관리자",
        birthDate = LocalDate.of(1985, 5, 10),
        gender = Gender.MALE,
        calendarType = CalendarType.SOLAR,
        role = Role.ADMIN
    )

    private val normalUser = User(
        id = 2L,
        name = "일반유저",
        birthDate = LocalDate.of(1990, 1, 15),
        gender = Gender.MALE,
        calendarType = CalendarType.SOLAR
    )

    @Test
    fun `getAppVersions - 관리자가 전체 버전 목록 조회 성공`() {
        val configs = listOf(
            AppVersionConfig(
                id = 1L,
                platform = "ios",
                minVersion = "1.0.0",
                storeUrl = "https://apps.apple.com/app/id000000000"
            ),
            AppVersionConfig(
                id = 2L,
                platform = "android",
                minVersion = "1.0.0",
                storeUrl = "https://play.google.com/store/apps/details?id=com.oyeapp.fortune"
            )
        )
        whenever(appVersionConfigRepository.findAll()).thenReturn(configs)

        val result = adminService.getAppVersions(adminUser)

        assertEquals(2, result.size)
        assertEquals("ios", result[0].platform)
        assertEquals("android", result[1].platform)
    }

    @Test
    fun `getAppVersions - 일반 유저가 조회 시 ForbiddenException`() {
        assertThrows<ForbiddenException> {
            adminService.getAppVersions(normalUser)
        }
    }

    @Test
    fun `updateAppVersion - 관리자가 버전 수정 성공`() {
        val config = AppVersionConfig(
            id = 1L,
            platform = "ios",
            minVersion = "1.0.0",
            storeUrl = "https://apps.apple.com/app/id000000000"
        )
        val request = AppVersionUpdateRequest(
            minVersion = "2.0.0",
            storeUrl = "https://apps.apple.com/app/id111111111"
        )

        whenever(appVersionConfigRepository.findByPlatform("ios")).thenReturn(config)
        whenever(appVersionConfigRepository.save(any<AppVersionConfig>())).thenAnswer { it.arguments[0] }

        val result = adminService.updateAppVersion(adminUser, "ios", request)

        assertEquals("2.0.0", result.minVersion)
        assertEquals("https://apps.apple.com/app/id111111111", result.storeUrl)
        assertEquals("ios", result.platform)
    }

    @Test
    fun `updateAppVersion - 일반 유저가 수정 시 ForbiddenException`() {
        val request = AppVersionUpdateRequest(minVersion = "2.0.0", storeUrl = "https://example.com")

        assertThrows<ForbiddenException> {
            adminService.updateAppVersion(normalUser, "ios", request)
        }
    }

    @Test
    fun `updateAppVersion - 존재하지 않는 플랫폼 수정 시 IllegalArgumentException`() {
        val request = AppVersionUpdateRequest(minVersion = "2.0.0", storeUrl = "https://example.com")

        whenever(appVersionConfigRepository.findByPlatform("windows")).thenReturn(null)

        assertThrows<IllegalArgumentException> {
            adminService.updateAppVersion(adminUser, "windows", request)
        }
    }
}
