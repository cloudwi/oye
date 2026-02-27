package com.mindbridge.oye.service

import com.mindbridge.oye.domain.PushNotification
import com.mindbridge.oye.domain.Role
import com.mindbridge.oye.domain.TargetType
import com.mindbridge.oye.domain.User
import com.mindbridge.oye.dto.PageResponse
import com.mindbridge.oye.dto.PushNotificationResponse
import com.mindbridge.oye.dto.SendPushRequest
import com.mindbridge.oye.exception.ForbiddenException
import com.mindbridge.oye.repository.PushNotificationRepository
import com.mindbridge.oye.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.client.RestTemplate

@Service
class PushNotificationService(
    private val pushNotificationRepository: PushNotificationRepository,
    private val userRepository: UserRepository,
    private val restTemplate: RestTemplate
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun updatePushToken(user: User, token: String?) {
        user.expoPushToken = token
        userRepository.save(user)
    }

    @Transactional
    fun sendPush(admin: User, request: SendPushRequest): PushNotificationResponse {
        requireAdmin(admin)

        val targetUsers = when (request.targetType) {
            TargetType.ALL -> userRepository.findAllByExpoPushTokenIsNotNull()
            TargetType.SPECIFIC -> {
                val ids = request.targetUserIds
                    ?: throw IllegalArgumentException("targetType이 SPECIFIC일 때 targetUserIds는 필수입니다.")
                userRepository.findAllByIdInAndExpoPushTokenIsNotNull(ids)
            }
        }

        val tokens = targetUsers.mapNotNull { it.expoPushToken }
        var sentCount = 0
        var failCount = 0

        tokens.chunked(100).forEach { batch ->
            val result = sendExpoPush(batch, request.title, request.body)
            sentCount += result.first
            failCount += result.second
        }

        val notification = PushNotification(
            title = request.title,
            body = request.body,
            targetType = request.targetType,
            targetUserIds = request.targetUserIds?.joinToString(","),
            sentCount = sentCount,
            failCount = failCount,
            sentBy = admin.id!!
        )
        val saved = pushNotificationRepository.save(notification)
        return PushNotificationResponse.from(saved)
    }

    @Transactional(readOnly = true)
    fun getPushHistory(admin: User, page: Int, size: Int): PageResponse<PushNotificationResponse> {
        requireAdmin(admin)
        val pageable = PageRequest.of(page, size)
        val pushPage = pushNotificationRepository.findAllByOrderByCreatedAtDesc(pageable)
        return PageResponse(
            content = pushPage.content.map { PushNotificationResponse.from(it) },
            page = pushPage.number,
            size = pushPage.size,
            totalElements = pushPage.totalElements,
            totalPages = pushPage.totalPages
        )
    }

    private fun sendExpoPush(tokens: List<String>, title: String, body: String): Pair<Int, Int> {
        val messages = tokens.map { token ->
            mapOf(
                "to" to token,
                "title" to title,
                "body" to body,
                "sound" to "default"
            )
        }

        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
        }
        val entity = HttpEntity(messages, headers)

        return try {
            val response = restTemplate.postForEntity(
                EXPO_PUSH_URL,
                entity,
                ExpoPushResponse::class.java
            )
            val data = response.body?.data ?: emptyList()
            val success = data.count { it.status == "ok" }
            val fail = data.size - success
            Pair(success, fail)
        } catch (e: Exception) {
            log.error("Expo Push API 호출 실패: {}", e.message, e)
            Pair(0, tokens.size)
        }
    }

    private fun requireAdmin(user: User) {
        if (user.role != Role.ADMIN) {
            throw ForbiddenException("관리자 권한이 필요합니다.")
        }
    }

    companion object {
        private const val EXPO_PUSH_URL = "https://exp.host/--/api/v2/push/send"
    }
}

data class ExpoPushResponse(
    val data: List<ExpoPushTicket> = emptyList()
)

data class ExpoPushTicket(
    val status: String = "",
    val id: String? = null,
    val message: String? = null
)
