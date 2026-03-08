package com.mindbridge.oye.service

import com.mindbridge.oye.domain.NotificationType
import com.mindbridge.oye.domain.User
import com.mindbridge.oye.domain.UserNotification
import com.mindbridge.oye.dto.PageResponse
import com.mindbridge.oye.dto.UnreadCountResponse
import com.mindbridge.oye.dto.UserNotificationResponse
import com.mindbridge.oye.exception.NotificationNotFoundException
import com.mindbridge.oye.repository.UserNotificationRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserNotificationService(
    private val userNotificationRepository: UserNotificationRepository
) {

    @Transactional(readOnly = true)
    fun getNotifications(user: User, page: Int, size: Int): PageResponse<UserNotificationResponse> {
        val pageable = PageRequest.of(page, size)
        val notificationPage = userNotificationRepository.findByUserOrderByCreatedAtDesc(user, pageable)
        return PageResponse(
            content = notificationPage.content.map { UserNotificationResponse.from(it) },
            page = notificationPage.number,
            size = notificationPage.size,
            totalElements = notificationPage.totalElements,
            totalPages = notificationPage.totalPages
        )
    }

    @Transactional(readOnly = true)
    fun getUnreadCount(user: User): UnreadCountResponse {
        val count = userNotificationRepository.countByUserAndIsReadFalse(user)
        return UnreadCountResponse(count)
    }

    @Transactional
    fun markAsRead(user: User, notificationId: Long) {
        val notification = userNotificationRepository.findById(notificationId)
            .orElseThrow { NotificationNotFoundException() }

        if (notification.user.id != user.id) {
            throw NotificationNotFoundException()
        }

        notification.isRead = true
    }

    @Transactional
    fun markAllAsRead(user: User) {
        userNotificationRepository.markAllAsReadByUser(user)
    }

    @Transactional
    fun createNotification(
        user: User,
        title: String,
        body: String,
        type: NotificationType = NotificationType.GENERAL,
        metadata: String? = null
    ): UserNotification {
        val notification = UserNotification(
            user = user,
            title = title,
            body = body,
            type = type,
            metadata = metadata
        )
        return userNotificationRepository.save(notification)
    }
}
