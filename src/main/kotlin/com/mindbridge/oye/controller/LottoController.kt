package com.mindbridge.oye.controller

import com.mindbridge.oye.config.AuthenticationResolver
import com.mindbridge.oye.controller.api.LottoApi
import com.mindbridge.oye.dto.ApiResponse
import com.mindbridge.oye.dto.LottoRecommendationResponse
import com.mindbridge.oye.dto.LottoRoundResponse
import com.mindbridge.oye.dto.LottoWinnerResponse
import com.mindbridge.oye.dto.PageResponse
import com.mindbridge.oye.service.LottoService
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/lotto")
class LottoController(
    private val lottoService: LottoService,
    private val authenticationResolver: AuthenticationResolver
) : LottoApi {

    @PostMapping("/recommendations")
    override fun recommend(
        @AuthenticationPrincipal principal: Any?,
        @RequestParam(required = false) round: Int?
    ): ApiResponse<List<LottoRecommendationResponse>> {
        val user = authenticationResolver.getCurrentUser(principal)
        val targetRound = round ?: lottoService.getCurrentRound()
        return ApiResponse.success(lottoService.recommend(user, targetRound))
    }

    @GetMapping("/recommendations")
    override fun getMyHistory(
        @AuthenticationPrincipal principal: Any?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ApiResponse<PageResponse<LottoRecommendationResponse>> {
        val user = authenticationResolver.getCurrentUser(principal)
        return ApiResponse.success(lottoService.getMyHistory(user, page, size))
    }

    @GetMapping("/winners")
    override fun getWinners(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ApiResponse<PageResponse<LottoWinnerResponse>> {
        return ApiResponse.success(lottoService.getWinners(page, size))
    }

    @GetMapping("/rounds/{round}")
    override fun getRound(
        @PathVariable round: Int
    ): ApiResponse<LottoRoundResponse> {
        return ApiResponse.success(lottoService.getRound(round))
    }
}
