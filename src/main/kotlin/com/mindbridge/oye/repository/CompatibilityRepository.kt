package com.mindbridge.oye.repository

import com.mindbridge.oye.domain.Compatibility
import com.mindbridge.oye.domain.UserConnection
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

interface CompatibilityRepository : JpaRepository<Compatibility, Long> {
    fun findByConnectionAndDate(connection: UserConnection, date: LocalDate): Compatibility?
    fun findByConnectionOrderByDateDesc(connection: UserConnection, pageable: Pageable): Page<Compatibility>
    fun deleteAllByConnection(connection: UserConnection)
    fun deleteAllByConnectionIn(connections: List<UserConnection>)
}
