package com.mindbridge.oye.repository

import com.mindbridge.oye.domain.PushNotification
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface PushNotificationRepository : JpaRepository<PushNotification, Long> {
    fun findAllByOrderByCreatedAtDesc(pageable: Pageable): Page<PushNotification>
}
