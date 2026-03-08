package com.mindbridge.oye.repository

import com.mindbridge.oye.domain.User
import com.mindbridge.oye.domain.UserNotification
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface UserNotificationRepository : JpaRepository<UserNotification, Long> {
    fun findByUserOrderByCreatedAtDesc(user: User, pageable: Pageable): Page<UserNotification>

    fun countByUserAndIsReadFalse(user: User): Long

    @Modifying
    @Query("UPDATE UserNotification n SET n.isRead = true WHERE n.user = :user AND n.isRead = false")
    fun markAllAsReadByUser(user: User)
}
