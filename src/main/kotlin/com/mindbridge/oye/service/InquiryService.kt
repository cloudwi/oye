package com.mindbridge.oye.service

import com.mindbridge.oye.domain.Inquiry
import com.mindbridge.oye.domain.InquiryComment
import com.mindbridge.oye.domain.InquiryStatus
import com.mindbridge.oye.domain.Role
import com.mindbridge.oye.domain.User
import com.mindbridge.oye.dto.InquiryCommentCreateRequest
import com.mindbridge.oye.dto.InquiryCreateRequest
import com.mindbridge.oye.dto.InquiryResponse
import com.mindbridge.oye.dto.PageResponse
import com.mindbridge.oye.exception.ForbiddenException
import com.mindbridge.oye.exception.InquiryNotFoundException
import com.mindbridge.oye.repository.InquiryCommentRepository
import com.mindbridge.oye.repository.InquiryRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class InquiryService(
    private val inquiryRepository: InquiryRepository,
    private val inquiryCommentRepository: InquiryCommentRepository
) {
    @Transactional
    fun createInquiry(user: User, request: InquiryCreateRequest): InquiryResponse {
        val inquiry = Inquiry(
            user = user,
            title = request.title,
            content = request.content
        )
        return InquiryResponse.from(inquiryRepository.save(inquiry))
    }

    @Transactional(readOnly = true)
    fun getMyInquiries(user: User, page: Int, size: Int): PageResponse<InquiryResponse> {
        val pageable = PageRequest.of(page, size)
        val inquiryPage = inquiryRepository.findByUserOrderByCreatedAtDesc(user, pageable)
        return PageResponse(
            content = inquiryPage.content.map { InquiryResponse.from(it) },
            page = inquiryPage.number,
            size = inquiryPage.size,
            totalElements = inquiryPage.totalElements,
            totalPages = inquiryPage.totalPages
        )
    }

    @Transactional(readOnly = true)
    fun getInquiry(user: User, inquiryId: Long): InquiryResponse {
        val inquiry = findInquiryById(inquiryId)

        if (inquiry.user.id != user.id && !isAdmin(user)) {
            throw ForbiddenException()
        }

        val comments = inquiryCommentRepository.findByInquiryOrderByCreatedAtAsc(inquiry)
        return InquiryResponse.from(inquiry, comments)
    }

    @Transactional
    fun addComment(user: User, inquiryId: Long, request: InquiryCommentCreateRequest): InquiryResponse {
        requireAdmin(user)

        val inquiry = findInquiryById(inquiryId)
        val comment = InquiryComment(
            inquiry = inquiry,
            admin = user,
            content = request.content
        )
        inquiryCommentRepository.save(comment)
        inquiry.status = InquiryStatus.ANSWERED

        val comments = inquiryCommentRepository.findByInquiryOrderByCreatedAtAsc(inquiry)
        return InquiryResponse.from(inquiry, comments)
    }

    private fun findInquiryById(inquiryId: Long): Inquiry {
        return inquiryRepository.findById(inquiryId)
            .orElseThrow { InquiryNotFoundException() }
    }

    private fun isAdmin(user: User): Boolean {
        return user.role == Role.ADMIN
    }

    private fun requireAdmin(user: User) {
        if (!isAdmin(user)) {
            throw ForbiddenException()
        }
    }
}
