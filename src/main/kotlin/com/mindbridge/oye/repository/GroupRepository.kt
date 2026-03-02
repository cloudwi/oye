package com.mindbridge.oye.repository

import com.mindbridge.oye.domain.Group
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.Optional

interface GroupRepository : JpaRepository<Group, Long> {
    fun findByInviteCode(inviteCode: String): Group?

    @Query("SELECT g FROM Group g JOIN FETCH g.owner WHERE g.id = :id")
    fun findByIdWithOwner(id: Long): Optional<Group>

    fun existsByInviteCode(inviteCode: String): Boolean
}
