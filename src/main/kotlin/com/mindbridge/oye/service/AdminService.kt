package com.mindbridge.oye.service

import com.mindbridge.oye.domain.InquiryStatus
import com.mindbridge.oye.domain.Role
import com.mindbridge.oye.domain.User
import com.mindbridge.oye.dto.AdminDashboardStats
import com.mindbridge.oye.dto.AdminUserResponse
import com.mindbridge.oye.dto.PageResponse
import com.mindbridge.oye.exception.ForbiddenException
import com.mindbridge.oye.exception.UserNotFoundException
import com.mindbridge.oye.repository.InquiryRepository
import com.mindbridge.oye.repository.UserRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AdminService(
    private val userRepository: UserRepository,
    private val inquiryRepository: InquiryRepository
) {
    @Transactional(readOnly = true)
    fun getStats(user: User): AdminDashboardStats {
        requireAdmin(user)
        return AdminDashboardStats(
            totalUsers = userRepository.count(),
            totalInquiries = inquiryRepository.count(),
            pendingInquiries = inquiryRepository.countByStatus(InquiryStatus.PENDING)
        )
    }

    @Transactional(readOnly = true)
    fun getUsers(user: User, page: Int, size: Int, search: String?): PageResponse<AdminUserResponse> {
        requireAdmin(user)
        val pageable = PageRequest.of(page, size)
        val userPage = if (!search.isNullOrBlank()) {
            userRepository.findByNameContainingIgnoreCaseOrderByCreatedAtDesc(search, pageable)
        } else {
            userRepository.findAllByOrderByCreatedAtDesc(pageable)
        }
        return PageResponse(
            content = userPage.content.map { AdminUserResponse.from(it) },
            page = userPage.number,
            size = userPage.size,
            totalElements = userPage.totalElements,
            totalPages = userPage.totalPages
        )
    }

    @Transactional
    fun updateUserRole(user: User, userId: Long, role: Role): AdminUserResponse {
        requireAdmin(user)
        val targetUser = userRepository.findById(userId)
            .orElseThrow { UserNotFoundException() }
        targetUser.role = role
        return AdminUserResponse.from(userRepository.save(targetUser))
    }

    private fun requireAdmin(user: User) {
        if (user.role != Role.ADMIN) {
            throw ForbiddenException("관리자 권한이 필요합니다.")
        }
    }
}
