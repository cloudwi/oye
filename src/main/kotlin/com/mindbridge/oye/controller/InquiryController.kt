package com.mindbridge.oye.controller

import com.mindbridge.oye.config.AuthenticationResolver
import com.mindbridge.oye.controller.api.InquiryApi
import com.mindbridge.oye.dto.ApiResponse
import com.mindbridge.oye.dto.InquiryCreateRequest
import com.mindbridge.oye.dto.InquiryReplyRequest
import com.mindbridge.oye.dto.InquiryResponse
import com.mindbridge.oye.dto.PageResponse
import com.mindbridge.oye.service.InquiryService
import jakarta.validation.Valid
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/inquiries")
class InquiryController(
    private val inquiryService: InquiryService,
    private val authenticationResolver: AuthenticationResolver
) : InquiryApi {

    @PostMapping
    override fun createInquiry(
        @AuthenticationPrincipal principal: Any?,
        @Valid @RequestBody request: InquiryCreateRequest
    ): InquiryResponse {
        val user = authenticationResolver.getCurrentUser(principal)
        return inquiryService.createInquiry(user, request)
    }

    @GetMapping
    override fun getMyInquiries(
        @AuthenticationPrincipal principal: Any?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ApiResponse<PageResponse<InquiryResponse>> {
        val user = authenticationResolver.getCurrentUser(principal)
        return ApiResponse.success(inquiryService.getMyInquiries(user, page, size))
    }

    @GetMapping("/{id}")
    override fun getInquiry(
        @AuthenticationPrincipal principal: Any?,
        @PathVariable id: Long
    ): InquiryResponse {
        val user = authenticationResolver.getCurrentUser(principal)
        return inquiryService.getInquiry(user, id)
    }

    @PostMapping("/{id}/reply")
    override fun replyToInquiry(
        @AuthenticationPrincipal principal: Any?,
        @PathVariable id: Long,
        @Valid @RequestBody request: InquiryReplyRequest
    ): InquiryResponse {
        val user = authenticationResolver.getCurrentUser(principal)
        return inquiryService.replyToInquiry(user, id, request)
    }
}
