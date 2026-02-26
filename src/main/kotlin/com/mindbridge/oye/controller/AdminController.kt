package com.mindbridge.oye.controller

import com.mindbridge.oye.config.AuthenticationResolver
import com.mindbridge.oye.controller.api.AdminApi
import com.mindbridge.oye.dto.AdminDashboardStats
import com.mindbridge.oye.dto.AdminUserResponse
import com.mindbridge.oye.dto.ApiResponse
import com.mindbridge.oye.dto.AppVersionConfigResponse
import com.mindbridge.oye.dto.AppVersionUpdateRequest
import com.mindbridge.oye.dto.PageResponse
import com.mindbridge.oye.dto.RoleUpdateRequest
import com.mindbridge.oye.service.AdminService
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/admin")
class AdminController(
    private val adminService: AdminService,
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
}
