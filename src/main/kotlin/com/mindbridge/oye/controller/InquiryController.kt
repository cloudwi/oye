package com.mindbridge.oye.controller

import com.mindbridge.oye.controller.api.InquiryApi
import com.mindbridge.oye.dto.ApiResponse
import com.mindbridge.oye.dto.InquiryCreateRequest
import com.mindbridge.oye.dto.InquiryReplyRequest
import com.mindbridge.oye.dto.InquiryResponse
import com.mindbridge.oye.dto.PageResponse
import com.mindbridge.oye.domain.User
import com.mindbridge.oye.exception.UnauthorizedException
import com.mindbridge.oye.exception.UserNotFoundException
import com.mindbridge.oye.repository.UserRepository
import com.mindbridge.oye.service.InquiryService
import jakarta.validation.Valid
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/inquiries")
class InquiryController(
    private val inquiryService: InquiryService,
    private val userRepository: UserRepository
) : InquiryApi {

    @PostMapping
    override fun createInquiry(
        @AuthenticationPrincipal principal: Any?,
        @Valid @RequestBody request: InquiryCreateRequest
    ): InquiryResponse {
        val user = getCurrentUser(principal)
        return inquiryService.createInquiry(user, request)
    }

    @GetMapping
    override fun getMyInquiries(
        @AuthenticationPrincipal principal: Any?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ApiResponse<PageResponse<InquiryResponse>> {
        val user = getCurrentUser(principal)
        val pageResponse = inquiryService.getMyInquiries(user, page, size)
        return ApiResponse.success(pageResponse)
    }

    @GetMapping("/{id}")
    override fun getInquiry(
        @AuthenticationPrincipal principal: Any?,
        @PathVariable id: Long
    ): InquiryResponse {
        val user = getCurrentUser(principal)
        return inquiryService.getInquiry(user, id)
    }

    @PostMapping("/{id}/reply")
    override fun replyToInquiry(
        @AuthenticationPrincipal principal: Any?,
        @PathVariable id: Long,
        @Valid @RequestBody request: InquiryReplyRequest
    ): InquiryResponse {
        val user = getCurrentUser(principal)
        return inquiryService.replyToInquiry(user, id, request)
    }

    private fun getCurrentUser(principal: Any?): User = when (principal) {
        is Long -> userRepository.findById(principal).orElseThrow { UserNotFoundException() }
        is OAuth2User -> {
            val userId = principal.attributes["userId"] as? Long
                ?: throw UnauthorizedException("사용자 ID를 찾을 수 없습니다.")
            userRepository.findById(userId).orElseThrow { UserNotFoundException() }
        }
        else -> throw UnauthorizedException()
    }
}
