package com.mindbridge.oye.repository

import com.mindbridge.oye.domain.Group
import com.mindbridge.oye.domain.GroupCompatibility
import com.mindbridge.oye.domain.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.LocalDate

interface GroupCompatibilityRepository : JpaRepository<GroupCompatibility, Long> {
    fun findByGroupAndDate(group: Group, date: LocalDate): List<GroupCompatibility>

    @Query(
        "SELECT gc FROM GroupCompatibility gc WHERE gc.group = :group AND gc.date = :date " +
            "AND gc.userA = :userA AND gc.userB = :userB"
    )
    fun findByGroupAndDateAndUsers(group: Group, date: LocalDate, userA: User, userB: User): GroupCompatibility?

    fun deleteAllByGroup(group: Group)
}
