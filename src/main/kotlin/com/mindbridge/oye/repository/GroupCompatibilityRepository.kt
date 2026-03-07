package com.mindbridge.oye.repository

import com.mindbridge.oye.domain.Group
import com.mindbridge.oye.domain.GroupCompatibility
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

interface GroupCompatibilityRepository : JpaRepository<GroupCompatibility, Long> {
    fun findByGroupAndDate(group: Group, date: LocalDate): GroupCompatibility?

    fun findByGroupAndDateGreaterThanEqualOrderByDateDesc(group: Group, since: LocalDate): List<GroupCompatibility>

    fun deleteAllByGroup(group: Group)
}
