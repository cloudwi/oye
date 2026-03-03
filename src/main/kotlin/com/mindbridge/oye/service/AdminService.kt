package com.mindbridge.oye.service

import com.mindbridge.oye.domain.InquiryStatus
import com.mindbridge.oye.domain.Role
import com.mindbridge.oye.domain.User
import com.mindbridge.oye.dto.AdminCompatibilityResponse
import com.mindbridge.oye.dto.AdminConnectionResponse
import com.mindbridge.oye.dto.AdminDashboardStats
import com.mindbridge.oye.dto.AdminFortuneResponse
import com.mindbridge.oye.dto.AdminGroupResponse
import com.mindbridge.oye.dto.AdminLottoResponse
import com.mindbridge.oye.dto.AdminUserDetailResponse
import com.mindbridge.oye.dto.AdminUserResponse
import com.mindbridge.oye.dto.AppVersionConfigResponse
import com.mindbridge.oye.dto.AppVersionUpdateRequest
import com.mindbridge.oye.dto.LoginHistoryResponse
import com.mindbridge.oye.dto.PageResponse
import com.mindbridge.oye.exception.ForbiddenException
import com.mindbridge.oye.exception.UserNotFoundException
import com.mindbridge.oye.repository.AppVersionConfigRepository
import com.mindbridge.oye.repository.CompatibilityRepository
import com.mindbridge.oye.repository.FortuneRepository
import com.mindbridge.oye.repository.GroupMemberRepository
import com.mindbridge.oye.repository.InquiryRepository
import com.mindbridge.oye.repository.LoginHistoryRepository
import com.mindbridge.oye.repository.LottoRecommendationRepository
import com.mindbridge.oye.repository.SocialAccountRepository
import com.mindbridge.oye.repository.UserConnectionRepository
import com.mindbridge.oye.repository.UserRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AdminService(
    private val userRepository: UserRepository,
    private val inquiryRepository: InquiryRepository,
    private val appVersionConfigRepository: AppVersionConfigRepository,
    private val loginHistoryRepository: LoginHistoryRepository,
    private val fortuneRepository: FortuneRepository,
    private val compatibilityRepository: CompatibilityRepository,
    private val lottoRecommendationRepository: LottoRecommendationRepository,
    private val userConnectionRepository: UserConnectionRepository,
    private val groupMemberRepository: GroupMemberRepository,
    private val socialAccountRepository: SocialAccountRepository
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

    @Transactional(readOnly = true)
    fun getAppVersions(user: User): List<AppVersionConfigResponse> {
        requireAdmin(user)
        return appVersionConfigRepository.findAll().map { AppVersionConfigResponse.from(it) }
    }

    @Transactional
    fun updateAppVersion(user: User, platform: String, request: AppVersionUpdateRequest): AppVersionConfigResponse {
        requireAdmin(user)
        val config = appVersionConfigRepository.findByPlatform(platform.lowercase())
            ?: throw IllegalArgumentException("플랫폼을 찾을 수 없습니다: $platform")
        config.minVersion = request.minVersion
        config.storeUrl = request.storeUrl
        config.updatedAt = java.time.LocalDateTime.now()
        return AppVersionConfigResponse.from(appVersionConfigRepository.save(config))
    }

    @Transactional(readOnly = true)
    fun getUserDetail(admin: User, userId: Long): AdminUserDetailResponse {
        requireAdmin(admin)
        val target = userRepository.findById(userId).orElseThrow { UserNotFoundException() }
        val provider = socialAccountRepository.findFirstByUser(target)?.provider
        return AdminUserDetailResponse(
            id = target.id!!,
            name = target.name,
            birthDate = target.birthDate,
            gender = target.gender,
            provider = provider,
            role = target.role,
            lastLoginAt = target.lastLoginAt,
            fortuneScheduleHour = target.fortuneScheduleHour,
            createdAt = target.createdAt
        )
    }

    @Transactional(readOnly = true)
    fun getLoginHistory(admin: User, userId: Long, page: Int, size: Int): PageResponse<LoginHistoryResponse> {
        requireAdmin(admin)
        val target = userRepository.findById(userId).orElseThrow { UserNotFoundException() }
        val historyPage = loginHistoryRepository.findByUserOrderByCreatedAtDesc(target, PageRequest.of(page, size))
        return PageResponse(
            content = historyPage.content.map {
                LoginHistoryResponse(
                    provider = it.provider,
                    ipAddress = it.ipAddress,
                    userAgent = it.userAgent,
                    createdAt = it.createdAt
                )
            },
            page = historyPage.number,
            size = historyPage.size,
            totalElements = historyPage.totalElements,
            totalPages = historyPage.totalPages
        )
    }

    @Transactional(readOnly = true)
    fun getUserFortunes(admin: User, userId: Long, page: Int, size: Int): PageResponse<AdminFortuneResponse> {
        requireAdmin(admin)
        val fortunePage = fortuneRepository.findByUserIdOrderByDateDesc(userId, PageRequest.of(page, size))
        return PageResponse(
            content = fortunePage.content.map {
                AdminFortuneResponse(date = it.date, score = it.score, content = it.content)
            },
            page = fortunePage.number,
            size = fortunePage.size,
            totalElements = fortunePage.totalElements,
            totalPages = fortunePage.totalPages
        )
    }

    @Transactional(readOnly = true)
    fun getUserCompatibilities(admin: User, userId: Long, page: Int, size: Int): PageResponse<AdminCompatibilityResponse> {
        requireAdmin(admin)
        val compatPage = compatibilityRepository.findByUserId(userId, PageRequest.of(page, size))
        return PageResponse(
            content = compatPage.content.map { c ->
                val conn = c.connection
                val partnerName = if (conn.user.id == userId) conn.partner.name else conn.user.name
                AdminCompatibilityResponse(
                    partnerName = partnerName,
                    relationType = conn.relationType,
                    date = c.date,
                    score = c.score,
                    content = c.content
                )
            },
            page = compatPage.number,
            size = compatPage.size,
            totalElements = compatPage.totalElements,
            totalPages = compatPage.totalPages
        )
    }

    @Transactional(readOnly = true)
    fun getUserLotto(admin: User, userId: Long, page: Int, size: Int): PageResponse<AdminLottoResponse> {
        requireAdmin(admin)
        val lottoPage = lottoRecommendationRepository.findByUserIdOrderByRoundDescSetNumberAsc(userId, PageRequest.of(page, size))
        return PageResponse(
            content = lottoPage.content.map {
                AdminLottoResponse(
                    round = it.round,
                    setNumber = it.setNumber,
                    numbers = it.numbers,
                    rank = it.rank?.name,
                    prizeAmount = it.prizeAmount,
                    evaluated = it.evaluated
                )
            },
            page = lottoPage.number,
            size = lottoPage.size,
            totalElements = lottoPage.totalElements,
            totalPages = lottoPage.totalPages
        )
    }

    @Transactional(readOnly = true)
    fun getUserConnections(admin: User, userId: Long): List<AdminConnectionResponse> {
        requireAdmin(admin)
        val target = userRepository.findById(userId).orElseThrow { UserNotFoundException() }
        val connections = userConnectionRepository.findByUserOrPartnerWithUsers(target)
        return connections.map { conn ->
            val isUser = conn.user.id == userId
            AdminConnectionResponse(
                partnerName = if (isUser) conn.partner.name else conn.user.name,
                partnerId = if (isUser) conn.partner.id!! else conn.user.id!!,
                relationType = conn.relationType
            )
        }
    }

    @Transactional(readOnly = true)
    fun getUserGroups(admin: User, userId: Long): List<AdminGroupResponse> {
        requireAdmin(admin)
        val target = userRepository.findById(userId).orElseThrow { UserNotFoundException() }
        val memberships = groupMemberRepository.findByUserWithGroup(target)
        return memberships.map { gm ->
            val memberCount = groupMemberRepository.countByGroup(gm.group)
            AdminGroupResponse(
                name = gm.group.name,
                memberCount = memberCount,
                isOwner = gm.group.owner.id == userId
            )
        }
    }

    private fun requireAdmin(user: User) {
        if (user.role != Role.ADMIN) {
            throw ForbiddenException("관리자 권한이 필요합니다.")
        }
    }
}
