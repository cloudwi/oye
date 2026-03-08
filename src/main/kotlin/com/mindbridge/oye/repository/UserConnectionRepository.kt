package com.mindbridge.oye.repository

import com.mindbridge.oye.domain.ConnectionStatus
import com.mindbridge.oye.domain.User
import com.mindbridge.oye.domain.UserConnection
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.Optional

interface UserConnectionRepository : JpaRepository<UserConnection, Long> {
    @Query("SELECT c FROM UserConnection c JOIN FETCH c.user JOIN FETCH c.partner WHERE c.id = :id")
    fun findByIdWithUsers(id: Long): Optional<UserConnection>

    @Query("SELECT c FROM UserConnection c JOIN FETCH c.user JOIN FETCH c.partner WHERE c.status = 'ACCEPTED'")
    fun findAllWithUsers(): List<UserConnection>

    @Query(
        value = "SELECT c FROM UserConnection c JOIN FETCH c.user JOIN FETCH c.partner WHERE c.status = 'ACCEPTED'",
        countQuery = "SELECT COUNT(c) FROM UserConnection c WHERE c.status = 'ACCEPTED'"
    )
    fun findAllWithUsers(pageable: Pageable): Page<UserConnection>

    @Query("SELECT c FROM UserConnection c JOIN FETCH c.user JOIN FETCH c.partner WHERE (c.user = :user OR c.partner = :user) AND c.status = 'ACCEPTED'")
    fun findByUserOrPartnerWithUsers(@Param("user") user: User): List<UserConnection>

    @Query("SELECT c FROM UserConnection c JOIN FETCH c.user JOIN FETCH c.partner WHERE c.partner = :user AND c.status = 'PENDING'")
    fun findPendingRequestsForUser(@Param("user") user: User): List<UserConnection>

    fun findByUserOrPartner(user: User, partner: User): List<UserConnection>
    fun existsByUserAndPartnerOrPartnerAndUser(user1: User, partner1: User, user2: User, partner2: User): Boolean
    fun deleteAllByUserOrPartner(user: User, partner: User)

    @Query("SELECT COUNT(c) > 0 FROM UserConnection c WHERE (c.user = :user OR c.partner = :user) AND c.relationType = com.mindbridge.oye.domain.RelationType.LOVER AND c.status = 'ACCEPTED'")
    fun existsLoverConnection(@Param("user") user: User): Boolean
}
