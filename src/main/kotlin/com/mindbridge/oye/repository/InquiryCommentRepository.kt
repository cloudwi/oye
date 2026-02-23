package com.mindbridge.oye.repository

import com.mindbridge.oye.domain.Inquiry
import com.mindbridge.oye.domain.InquiryComment
import org.springframework.data.jpa.repository.JpaRepository

interface InquiryCommentRepository : JpaRepository<InquiryComment, Long> {
    fun findByInquiryOrderByCreatedAtAsc(inquiry: Inquiry): List<InquiryComment>
    fun deleteAllByInquiry(inquiry: Inquiry)
}
