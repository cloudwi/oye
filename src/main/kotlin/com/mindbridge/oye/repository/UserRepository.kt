package com.mindbridge.oye.repository

import com.mindbridge.oye.domain.User
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface UserRepository : JpaRepository<User, Long> {
    fun findByConnectCode(code: String): User?
    fun findAllByOrderByCreatedAtDesc(pageable: Pageable): Page<User>
    fun findByNameContainingIgnoreCaseOrderByCreatedAtDesc(name: String, pageable: Pageable): Page<User>
}
