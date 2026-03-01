package com.mindbridge.oye.repository

import com.mindbridge.oye.domain.Compatibility
import com.mindbridge.oye.domain.UserConnection
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.LocalDate

interface CompatibilityRepository : JpaRepository<Compatibility, Long> {
    fun findByConnectionAndDate(connection: UserConnection, date: LocalDate): Compatibility?
    fun findByConnectionInAndDate(connections: List<UserConnection>, date: LocalDate): List<Compatibility>
    fun findByConnectionOrderByDateDesc(connection: UserConnection, pageable: Pageable): Page<Compatibility>
    fun findByConnectionAndDateBetweenOrderByDateAsc(connection: UserConnection, start: LocalDate, end: LocalDate): List<Compatibility>

    @Query("SELECT c.date FROM Compatibility c WHERE c.connection = :connection AND c.date BETWEEN :start AND :end ORDER BY c.date ASC")
    fun findDatesByConnectionAndDateBetween(connection: UserConnection, start: LocalDate, end: LocalDate): List<LocalDate>

    fun deleteAllByConnection(connection: UserConnection)
    fun deleteAllByConnectionIn(connections: List<UserConnection>)
}
