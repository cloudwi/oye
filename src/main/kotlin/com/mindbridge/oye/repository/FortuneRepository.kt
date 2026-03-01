package com.mindbridge.oye.repository

import com.mindbridge.oye.domain.Fortune
import com.mindbridge.oye.domain.User
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.time.LocalDate

interface FortuneRepository : JpaRepository<Fortune, Long> {
    fun findByUserAndDate(user: User, date: LocalDate): Fortune?
    fun findByUserOrderByDateDesc(user: User, pageable: Pageable): Page<Fortune>
    fun findByUserAndDateBetweenOrderByDateAsc(user: User, start: LocalDate, end: LocalDate): List<Fortune>

    @Query("SELECT f.date FROM Fortune f WHERE f.user = :user AND f.date BETWEEN :start AND :end ORDER BY f.date ASC")
    fun findDatesByUserAndDateBetween(user: User, start: LocalDate, end: LocalDate): List<LocalDate>

    fun findByScoreIsNull(): List<Fortune>

    @Modifying
    @Query("UPDATE Fortune f SET f.score = :score WHERE f.id = :id")
    fun updateScore(id: Long, score: Int)

    fun deleteAllByUser(user: User)
}
