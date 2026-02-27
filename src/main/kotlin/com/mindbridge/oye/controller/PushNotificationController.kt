package com.mindbridge.oye.controller

import com.mindbridge.oye.config.AuthenticationResolver
import com.mindbridge.oye.controller.api.PushNotificationAdminApi
import com.mindbridge.oye.dto.ApiResponse
import com.mindbridge.oye.dto.PageResponse
import com.mindbridge.oye.dto.PushNotificationResponse
import com.mindbridge.oye.dto.SendPushRequest
import com.mindbridge.oye.service.PushNotificationService
import jakarta.validation.Valid
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/admin/push")
class PushNotificationController(
    private val pushNotificationService: PushNotificationService,
    private val authenticationResolver: AuthenticationResolver
) : PushNotificationAdminApi {

    @PostMapping
    override fun sendPush(
        @AuthenticationPrincipal principal: Any?,
        @Valid @RequestBody request: SendPushRequest
    ): PushNotificationResponse {
        val user = authenticationResolver.getCurrentUser(principal)
        return pushNotificationService.sendPush(user, request)
    }

    @GetMapping
    override fun getPushHistory(
        @AuthenticationPrincipal principal: Any?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ApiResponse<PageResponse<PushNotificationResponse>> {
        val user = authenticationResolver.getCurrentUser(principal)
        return ApiResponse.success(pushNotificationService.getPushHistory(user, page, size))
    }
}
