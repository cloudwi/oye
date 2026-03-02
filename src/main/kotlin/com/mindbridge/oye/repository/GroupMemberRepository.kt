package com.mindbridge.oye.repository

import com.mindbridge.oye.domain.Group
import com.mindbridge.oye.domain.GroupMember
import com.mindbridge.oye.domain.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface GroupMemberRepository : JpaRepository<GroupMember, Long> {
    @Query("SELECT gm FROM GroupMember gm JOIN FETCH gm.group JOIN FETCH gm.group.owner WHERE gm.user = :user")
    fun findByUserWithGroup(user: User): List<GroupMember>

    @Query("SELECT gm FROM GroupMember gm JOIN FETCH gm.user WHERE gm.group = :group")
    fun findByGroupWithUsers(group: Group): List<GroupMember>

    fun countByGroup(group: Group): Long

    fun existsByGroupAndUser(group: Group, user: User): Boolean

    fun deleteByGroupAndUser(group: Group, user: User)
}
