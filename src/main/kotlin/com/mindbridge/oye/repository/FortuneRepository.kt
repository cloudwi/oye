package com.mindbridge.oye.repository

import com.mindbridge.oye.domain.Fortune
import com.mindbridge.oye.domain.User
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

interface FortuneRepository : JpaRepository<Fortune, Long> {
    fun findByUserAndDate(user: User, date: LocalDate): Fortune?
    fun findByUserOrderByDateDesc(user: User): List<Fortune>
    fun findByUserOrderByDateDesc(user: User, pageable: Pageable): Page<Fortune>
    fun deleteAllByUser(user: User)
}
