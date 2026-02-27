package com.mindbridge.oye.service

import com.mindbridge.oye.domain.CalendarType
import com.mindbridge.oye.domain.Gender
import com.mindbridge.oye.domain.PushNotification
import com.mindbridge.oye.domain.Role
import com.mindbridge.oye.domain.TargetType
import com.mindbridge.oye.domain.User
import com.mindbridge.oye.dto.SendPushRequest
import com.mindbridge.oye.exception.ForbiddenException
import com.mindbridge.oye.repository.PushNotificationRepository
import com.mindbridge.oye.repository.UserRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestTemplate
import java.time.LocalDate

@ExtendWith(MockitoExtension::class)
class PushNotificationServiceTest {

    @Mock
    private lateinit var pushNotificationRepository: PushNotificationRepository

    @Mock
    private lateinit var userRepository: UserRepository

    @Mock
    private lateinit var restTemplate: RestTemplate

    @InjectMocks
    private lateinit var pushNotificationService: PushNotificationService

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
    fun `updatePushToken - 토큰 등록 성공`() {
        val user = User(
            id = 3L,
            name = "테스트유저",
            birthDate = LocalDate.of(1995, 3, 20)
        )
        whenever(userRepository.save(any<User>())).thenAnswer { it.arguments[0] }

        pushNotificationService.updatePushToken(user, "ExponentPushToken[test123]")

        assertEquals("ExponentPushToken[test123]", user.expoPushToken)
    }

    @Test
    fun `updatePushToken - 토큰 해제 성공`() {
        val user = User(
            id = 3L,
            name = "테스트유저",
            birthDate = LocalDate.of(1995, 3, 20),
            expoPushToken = "ExponentPushToken[test123]"
        )
        whenever(userRepository.save(any<User>())).thenAnswer { it.arguments[0] }

        pushNotificationService.updatePushToken(user, null)

        assertEquals(null, user.expoPushToken)
    }

    @Test
    fun `sendPush - 일반 유저가 발송 시 ForbiddenException`() {
        val request = SendPushRequest(
            title = "테스트",
            body = "테스트 메시지",
            targetType = TargetType.ALL
        )

        assertThrows<ForbiddenException> {
            pushNotificationService.sendPush(normalUser, request)
        }
    }

    @Test
    fun `sendPush - 전체 발송 성공`() {
        val usersWithTokens = listOf(
            User(id = 10L, name = "유저1", birthDate = LocalDate.of(1990, 1, 1), expoPushToken = "ExponentPushToken[aaa]"),
            User(id = 11L, name = "유저2", birthDate = LocalDate.of(1991, 2, 2), expoPushToken = "ExponentPushToken[bbb]")
        )
        whenever(userRepository.findAllByExpoPushTokenIsNotNull()).thenReturn(usersWithTokens)

        val expoResponse = ExpoPushResponse(
            data = listOf(
                ExpoPushTicket(status = "ok", id = "ticket1"),
                ExpoPushTicket(status = "ok", id = "ticket2")
            )
        )
        whenever(restTemplate.postForEntity(any<String>(), any(), eq(ExpoPushResponse::class.java)))
            .thenReturn(ResponseEntity.ok(expoResponse))

        val notificationCaptor = argumentCaptor<PushNotification>()
        whenever(pushNotificationRepository.save(notificationCaptor.capture())).thenAnswer {
            val n = it.arguments[0] as PushNotification
            PushNotification(
                id = 1L,
                title = n.title,
                body = n.body,
                targetType = n.targetType,
                targetUserIds = n.targetUserIds,
                sentCount = n.sentCount,
                failCount = n.failCount,
                sentBy = n.sentBy,
                createdAt = n.createdAt
            )
        }

        val request = SendPushRequest(
            title = "공지사항",
            body = "새로운 기능이 추가되었습니다!",
            targetType = TargetType.ALL
        )
        val result = pushNotificationService.sendPush(adminUser, request)

        assertEquals(2, result.sentCount)
        assertEquals(0, result.failCount)
        assertEquals("공지사항", result.title)
        assertEquals(TargetType.ALL, result.targetType)
    }

    @Test
    fun `sendPush - 특정 사용자 발송 성공`() {
        val usersWithTokens = listOf(
            User(id = 10L, name = "유저1", birthDate = LocalDate.of(1990, 1, 1), expoPushToken = "ExponentPushToken[aaa]")
        )
        whenever(userRepository.findAllByIdInAndExpoPushTokenIsNotNull(listOf(10L))).thenReturn(usersWithTokens)

        val expoResponse = ExpoPushResponse(
            data = listOf(ExpoPushTicket(status = "ok", id = "ticket1"))
        )
        whenever(restTemplate.postForEntity(any<String>(), any(), eq(ExpoPushResponse::class.java)))
            .thenReturn(ResponseEntity.ok(expoResponse))

        whenever(pushNotificationRepository.save(any<PushNotification>())).thenAnswer {
            val n = it.arguments[0] as PushNotification
            PushNotification(
                id = 2L,
                title = n.title,
                body = n.body,
                targetType = n.targetType,
                targetUserIds = n.targetUserIds,
                sentCount = n.sentCount,
                failCount = n.failCount,
                sentBy = n.sentBy,
                createdAt = n.createdAt
            )
        }

        val request = SendPushRequest(
            title = "개인 알림",
            body = "특별 메시지",
            targetType = TargetType.SPECIFIC,
            targetUserIds = listOf(10L)
        )
        val result = pushNotificationService.sendPush(adminUser, request)

        assertEquals(1, result.sentCount)
        assertEquals(0, result.failCount)
        assertEquals(TargetType.SPECIFIC, result.targetType)
        assertEquals(listOf(10L), result.targetUserIds)
    }

    @Test
    fun `sendPush - Expo API 실패 시 failCount 증가`() {
        val usersWithTokens = listOf(
            User(id = 10L, name = "유저1", birthDate = LocalDate.of(1990, 1, 1), expoPushToken = "ExponentPushToken[aaa]"),
            User(id = 11L, name = "유저2", birthDate = LocalDate.of(1991, 2, 2), expoPushToken = "ExponentPushToken[bbb]")
        )
        whenever(userRepository.findAllByExpoPushTokenIsNotNull()).thenReturn(usersWithTokens)

        whenever(restTemplate.postForEntity(any<String>(), any(), eq(ExpoPushResponse::class.java)))
            .thenThrow(RuntimeException("Expo API 오류"))

        whenever(pushNotificationRepository.save(any<PushNotification>())).thenAnswer {
            val n = it.arguments[0] as PushNotification
            PushNotification(
                id = 3L,
                title = n.title,
                body = n.body,
                targetType = n.targetType,
                targetUserIds = n.targetUserIds,
                sentCount = n.sentCount,
                failCount = n.failCount,
                sentBy = n.sentBy,
                createdAt = n.createdAt
            )
        }

        val request = SendPushRequest(
            title = "테스트",
            body = "실패 테스트",
            targetType = TargetType.ALL
        )
        val result = pushNotificationService.sendPush(adminUser, request)

        assertEquals(0, result.sentCount)
        assertEquals(2, result.failCount)
    }

    @Test
    fun `sendPush - SPECIFIC 타입에 targetUserIds 없으면 IllegalArgumentException`() {
        val request = SendPushRequest(
            title = "테스트",
            body = "테스트",
            targetType = TargetType.SPECIFIC,
            targetUserIds = null
        )

        assertThrows<IllegalArgumentException> {
            pushNotificationService.sendPush(adminUser, request)
        }
    }
}
