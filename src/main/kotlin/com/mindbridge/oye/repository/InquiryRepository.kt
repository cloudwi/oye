package com.mindbridge.oye.repository

import com.mindbridge.oye.domain.Inquiry
import com.mindbridge.oye.domain.User
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface InquiryRepository : JpaRepository<Inquiry, Long> {
    fun findByUserOrderByCreatedAtDesc(user: User, pageable: Pageable): Page<Inquiry>
    fun deleteAllByUser(user: User)
}
