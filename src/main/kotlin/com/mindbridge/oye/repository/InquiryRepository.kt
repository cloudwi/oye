package com.mindbridge.oye.repository

import com.mindbridge.oye.domain.Inquiry
import com.mindbridge.oye.domain.InquiryStatus
import com.mindbridge.oye.domain.User
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface InquiryRepository : JpaRepository<Inquiry, Long> {
    fun findByUserOrderByCreatedAtDesc(user: User, pageable: Pageable): Page<Inquiry>

    @Query("SELECT i FROM Inquiry i JOIN FETCH i.user ORDER BY i.createdAt DESC",
           countQuery = "SELECT COUNT(i) FROM Inquiry i")
    fun findAllWithUserOrderByCreatedAtDesc(pageable: Pageable): Page<Inquiry>

    fun findAllByOrderByCreatedAtDesc(pageable: Pageable): Page<Inquiry>
    fun findAllByUser(user: User): List<Inquiry>

    @Query("SELECT i FROM Inquiry i JOIN FETCH i.user WHERE i.user.id = :userId ORDER BY i.createdAt DESC",
           countQuery = "SELECT COUNT(i) FROM Inquiry i WHERE i.user.id = :userId")
    fun findByUserIdWithUserOrderByCreatedAtDesc(userId: Long, pageable: Pageable): Page<Inquiry>

    fun deleteAllByUser(user: User)
    fun countByStatus(status: InquiryStatus): Long
}
