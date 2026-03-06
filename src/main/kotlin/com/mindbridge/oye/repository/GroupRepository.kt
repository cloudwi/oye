package com.mindbridge.oye.repository

import com.mindbridge.oye.domain.Group
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.Optional

interface GroupRepository : JpaRepository<Group, Long> {
    @Query("SELECT g FROM Group g JOIN FETCH g.owner WHERE g.inviteCode = :inviteCode")
    fun findByInviteCode(inviteCode: String): Group?

    @Query("SELECT g FROM Group g JOIN FETCH g.owner WHERE g.id = :id")
    fun findByIdWithOwner(id: Long): Optional<Group>

    @Query(
        value = "SELECT g FROM Group g JOIN FETCH g.owner",
        countQuery = "SELECT COUNT(g) FROM Group g"
    )
    fun findAllWithOwner(pageable: Pageable): Page<Group>

    fun existsByInviteCode(inviteCode: String): Boolean
}
