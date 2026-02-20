package com.mindbridge.oye.controller

import com.mindbridge.oye.config.AuthenticationResolver
import com.mindbridge.oye.controller.api.FortuneApi
import com.mindbridge.oye.dto.ApiResponse
import com.mindbridge.oye.dto.FortuneResponse
import com.mindbridge.oye.dto.PageResponse
import com.mindbridge.oye.service.FortuneService
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/fortune")
class FortuneController(
    private val fortuneService: FortuneService,
    private val authenticationResolver: AuthenticationResolver
) : FortuneApi {

    @GetMapping("/today")
    override fun getTodayFortune(@AuthenticationPrincipal principal: Any?): FortuneResponse {
        val user = authenticationResolver.getCurrentUser(principal)
        return FortuneResponse.from(fortuneService.generateFortune(user))
    }

    @GetMapping("/history")
    override fun getFortuneHistory(
        @AuthenticationPrincipal principal: Any?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ApiResponse<PageResponse<FortuneResponse>> {
        val user = authenticationResolver.getCurrentUser(principal)
        return ApiResponse.success(fortuneService.getFortuneHistory(user, page, size))
    }
}
