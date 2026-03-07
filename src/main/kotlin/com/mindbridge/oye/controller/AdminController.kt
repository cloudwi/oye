package com.mindbridge.oye.controller

import com.mindbridge.oye.config.AuthenticationResolver
import com.mindbridge.oye.controller.api.AdminApi
import com.mindbridge.oye.dto.AdminCompatibilityResponse
import com.mindbridge.oye.dto.AdminConnectionResponse
import com.mindbridge.oye.dto.AdminDashboardStats
import com.mindbridge.oye.dto.AdminFortuneResponse
import com.mindbridge.oye.dto.AdminGroupResponse
import com.mindbridge.oye.dto.AdminLottoResponse
import com.mindbridge.oye.dto.AdminUserDetailResponse
import com.mindbridge.oye.dto.AdminUserResponse
import com.mindbridge.oye.dto.ApiResponse
import com.mindbridge.oye.dto.AppVersionConfigResponse
import com.mindbridge.oye.dto.AppVersionUpdateRequest
import com.mindbridge.oye.dto.LoginHistoryResponse
import com.mindbridge.oye.dto.PageResponse
import com.mindbridge.oye.dto.RoleUpdateRequest
import com.mindbridge.oye.service.AdminService
import com.mindbridge.oye.service.DailyFortuneScheduler
import com.mindbridge.oye.service.LottoDrawService
import com.mindbridge.oye.repository.LottoRecommendationRepository
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/admin")
class AdminController(
    private val adminService: AdminService,
    private val dailyFortuneScheduler: DailyFortuneScheduler,
    private val lottoDrawService: LottoDrawService,
    private val lottoRecommendationRepository: LottoRecommendationRepository,
    private val authenticationResolver: AuthenticationResolver
) : AdminApi {

    @GetMapping("/stats")
    override fun getStats(
        @AuthenticationPrincipal principal: Any?
    ): AdminDashboardStats {
        val user = authenticationResolver.getCurrentUser(principal)
        return adminService.getStats(user)
    }

    @GetMapping("/users")
    override fun getUsers(
        @AuthenticationPrincipal principal: Any?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(required = false) search: String?
    ): ApiResponse<PageResponse<AdminUserResponse>> {
        val user = authenticationResolver.getCurrentUser(principal)
        return ApiResponse.success(adminService.getUsers(user, page, size, search))
    }

    @PatchMapping("/users/{id}/role")
    override fun updateUserRole(
        @AuthenticationPrincipal principal: Any?,
        @PathVariable id: Long,
        @RequestBody request: RoleUpdateRequest
    ): AdminUserResponse {
        val user = authenticationResolver.getCurrentUser(principal)
        return adminService.updateUserRole(user, id, request.role)
    }

    @GetMapping("/app-versions")
    override fun getAppVersions(
        @AuthenticationPrincipal principal: Any?
    ): List<AppVersionConfigResponse> {
        val user = authenticationResolver.getCurrentUser(principal)
        return adminService.getAppVersions(user)
    }

    @PutMapping("/app-versions/{platform}")
    override fun updateAppVersion(
        @AuthenticationPrincipal principal: Any?,
        @PathVariable platform: String,
        @RequestBody request: AppVersionUpdateRequest
    ): AppVersionConfigResponse {
        val user = authenticationResolver.getCurrentUser(principal)
        return adminService.updateAppVersion(user, platform, request)
    }

    @GetMapping("/users/{id}")
    override fun getUserDetail(
        @AuthenticationPrincipal principal: Any?,
        @PathVariable id: Long
    ): AdminUserDetailResponse {
        val user = authenticationResolver.getCurrentUser(principal)
        return adminService.getUserDetail(user, id)
    }

    @GetMapping("/users/{id}/login-history")
    override fun getUserLoginHistory(
        @AuthenticationPrincipal principal: Any?,
        @PathVariable id: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ApiResponse<PageResponse<LoginHistoryResponse>> {
        val user = authenticationResolver.getCurrentUser(principal)
        return ApiResponse.success(adminService.getLoginHistory(user, id, page, size))
    }

    @GetMapping("/users/{id}/fortunes")
    override fun getUserFortunes(
        @AuthenticationPrincipal principal: Any?,
        @PathVariable id: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ApiResponse<PageResponse<AdminFortuneResponse>> {
        val user = authenticationResolver.getCurrentUser(principal)
        return ApiResponse.success(adminService.getUserFortunes(user, id, page, size))
    }

    @GetMapping("/users/{id}/compatibilities")
    override fun getUserCompatibilities(
        @AuthenticationPrincipal principal: Any?,
        @PathVariable id: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ApiResponse<PageResponse<AdminCompatibilityResponse>> {
        val user = authenticationResolver.getCurrentUser(principal)
        return ApiResponse.success(adminService.getUserCompatibilities(user, id, page, size))
    }

    @GetMapping("/users/{id}/lotto")
    override fun getUserLotto(
        @AuthenticationPrincipal principal: Any?,
        @PathVariable id: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ApiResponse<PageResponse<AdminLottoResponse>> {
        val user = authenticationResolver.getCurrentUser(principal)
        return ApiResponse.success(adminService.getUserLotto(user, id, page, size))
    }

    @GetMapping("/users/{id}/connections")
    override fun getUserConnections(
        @AuthenticationPrincipal principal: Any?,
        @PathVariable id: Long
    ): List<AdminConnectionResponse> {
        val user = authenticationResolver.getCurrentUser(principal)
        return adminService.getUserConnections(user, id)
    }

    @GetMapping("/users/{id}/groups")
    override fun getUserGroups(
        @AuthenticationPrincipal principal: Any?,
        @PathVariable id: Long
    ): List<AdminGroupResponse> {
        val user = authenticationResolver.getCurrentUser(principal)
        return adminService.getUserGroups(user, id)
    }

    @PostMapping("/generate-daily")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun triggerDailyGeneration(@AuthenticationPrincipal principal: Any?) {
        val user = authenticationResolver.getCurrentUser(principal)
        adminService.requireAdmin(user)
        dailyFortuneScheduler.generateDailyFortunes()
        dailyFortuneScheduler.generateDailyCompatibilities()
        dailyFortuneScheduler.generateDailyGroupCompatibilities()
    }

    @PostMapping("/evaluate-lotto")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun triggerLottoEvaluation(@AuthenticationPrincipal principal: Any?) {
        val user = authenticationResolver.getCurrentUser(principal)
        adminService.requireAdmin(user)
        val unevaluatedRounds = lottoRecommendationRepository.findDistinctUnevaluatedRounds()
        for (round in unevaluatedRounds) {
            try {
                val lottoRound = lottoDrawService.fetchDrawResult(round)
                lottoDrawService.evaluateRecommendations(lottoRound)
            } catch (e: Exception) {
                // 아직 추첨 결과가 없는 회차는 건너뜀
            }
        }
    }
}
