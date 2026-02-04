package com.mindbridge.oye.controller

import com.mindbridge.oye.dto.FortuneResponse
import com.mindbridge.oye.exception.UnauthorizedException
import com.mindbridge.oye.exception.UserNotFoundException
import com.mindbridge.oye.repository.UserRepository
import com.mindbridge.oye.service.FortuneService
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/fortune")
class FortuneController(
    private val fortuneService: FortuneService,
    private val userRepository: UserRepository
) {

    @GetMapping("/today")
    fun getTodayFortune(@AuthenticationPrincipal principal: OAuth2User?): FortuneResponse {
        val user = getCurrentUser(principal)
        val fortune = fortuneService.generateFortune(user)
        return FortuneResponse.from(fortune)
    }

    @GetMapping("/history")
    fun getFortuneHistory(@AuthenticationPrincipal principal: OAuth2User?): List<FortuneResponse> {
        val user = getCurrentUser(principal)
        return fortuneService.getFortuneHistory(user).map { FortuneResponse.from(it) }
    }

    private fun getCurrentUser(principal: OAuth2User?) =
        principal?.let {
            val userId = it.attributes["userId"] as? Long
                ?: throw UnauthorizedException("사용자 ID를 찾을 수 없습니다.")
            userRepository.findById(userId).orElseThrow { UserNotFoundException() }
        } ?: throw UnauthorizedException()
}
