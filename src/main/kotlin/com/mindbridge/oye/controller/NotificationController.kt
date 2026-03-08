package com.mindbridge.oye.controller

import com.mindbridge.oye.config.AuthenticationResolver
import com.mindbridge.oye.controller.api.NotificationApi
import com.mindbridge.oye.dto.ApiResponse
import com.mindbridge.oye.dto.PageResponse
import com.mindbridge.oye.dto.UnreadCountResponse
import com.mindbridge.oye.dto.UserNotificationResponse
import com.mindbridge.oye.service.UserNotificationService
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/notifications")
class NotificationController(
    private val userNotificationService: UserNotificationService,
    private val authenticationResolver: AuthenticationResolver
) : NotificationApi {

    @GetMapping
    override fun getNotifications(
        @AuthenticationPrincipal principal: Any?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ApiResponse<PageResponse<UserNotificationResponse>> {
        val user = authenticationResolver.getCurrentUser(principal)
        return ApiResponse.success(userNotificationService.getNotifications(user, page, size))
    }

    @GetMapping("/unread-count")
    override fun getUnreadCount(
        @AuthenticationPrincipal principal: Any?
    ): ApiResponse<UnreadCountResponse> {
        val user = authenticationResolver.getCurrentUser(principal)
        return ApiResponse.success(userNotificationService.getUnreadCount(user))
    }

    @PatchMapping("/{id}/read")
    override fun markAsRead(
        @AuthenticationPrincipal principal: Any?,
        @PathVariable id: Long
    ): ApiResponse<Any?> {
        val user = authenticationResolver.getCurrentUser(principal)
        userNotificationService.markAsRead(user, id)
        return ApiResponse.success(null as Any?)
    }

    @PatchMapping("/read-all")
    override fun markAllAsRead(
        @AuthenticationPrincipal principal: Any?
    ): ApiResponse<Any?> {
        val user = authenticationResolver.getCurrentUser(principal)
        userNotificationService.markAllAsRead(user)
        return ApiResponse.success(null as Any?)
    }
}
