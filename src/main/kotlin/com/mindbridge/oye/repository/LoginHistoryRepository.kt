package com.mindbridge.oye.repository

import com.mindbridge.oye.domain.LoginHistory
import com.mindbridge.oye.domain.User
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface LoginHistoryRepository : JpaRepository<LoginHistory, Long> {
    fun findByUserOrderByCreatedAtDesc(user: User, pageable: Pageable): Page<LoginHistory>
    fun deleteAllByUser(user: User)
}
