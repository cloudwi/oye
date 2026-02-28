package com.mindbridge.oye.controller

import com.mindbridge.oye.config.AuthenticationResolver
import com.mindbridge.oye.controller.api.CompatibilityApi
import com.mindbridge.oye.dto.ApiResponse
import com.mindbridge.oye.dto.CompatibilityResponse
import com.mindbridge.oye.dto.PageResponse
import com.mindbridge.oye.service.CompatibilityService
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/connections/{id}/compatibility")
class CompatibilityController(
    private val compatibilityService: CompatibilityService,
    private val authenticationResolver: AuthenticationResolver
) : CompatibilityApi {

    @GetMapping
    override fun getCompatibility(
        @AuthenticationPrincipal principal: Any?,
        @PathVariable id: Long
    ): ApiResponse<CompatibilityResponse> {
        val user = authenticationResolver.getCurrentUser(principal)
        return ApiResponse.success(compatibilityService.getCompatibility(user, id))
    }

    @GetMapping("/history")
    override fun getCompatibilityHistory(
        @AuthenticationPrincipal principal: Any?,
        @PathVariable id: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ApiResponse<PageResponse<CompatibilityResponse>> {
        val user = authenticationResolver.getCurrentUser(principal)
        return ApiResponse.success(compatibilityService.getCompatibilityHistory(user, id, page, size))
    }
}
