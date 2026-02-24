package com.mindbridge.oye.repository

import com.mindbridge.oye.domain.User
import com.mindbridge.oye.domain.UserConnection
import org.springframework.data.jpa.repository.JpaRepository

interface UserConnectionRepository : JpaRepository<UserConnection, Long> {
    fun findByUserOrPartner(user: User, partner: User): List<UserConnection>
    fun existsByUserAndPartnerOrPartnerAndUser(user1: User, partner1: User, user2: User, partner2: User): Boolean
    fun deleteAllByUserOrPartner(user: User, partner: User)
}
