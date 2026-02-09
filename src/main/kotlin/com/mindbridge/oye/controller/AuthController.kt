package com.mindbridge.oye.controller

import com.mindbridge.oye.config.JwtTokenProvider
import com.mindbridge.oye.exception.UnauthorizedException
import jakarta.servlet.http.HttpServletRequest
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.view.RedirectView

data class RefreshTokenRequest(val refreshToken: String)
data class TokenResponse(val accessToken: String, val refreshToken: String)

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val jwtTokenProvider: JwtTokenProvider
) {

    @GetMapping("/login/kakao")
    fun loginKakao(
        @RequestParam(defaultValue = "web") platform: String,
        @RequestParam(name = "redirect_uri", required = false) redirectUri: String?,
        request: HttpServletRequest
    ): RedirectView {
        val session = request.session
        session.setAttribute("oauth2_platform", platform)
        if (redirectUri != null) {
            session.setAttribute("oauth2_redirect_uri", redirectUri)
        }
        return RedirectView("/oauth2/authorization/kakao")
    }

    @PostMapping("/refresh")
    fun refresh(@RequestBody request: RefreshTokenRequest): TokenResponse {
        if (!jwtTokenProvider.validateToken(request.refreshToken)) {
            throw UnauthorizedException("유효하지 않은 리프레시 토큰입니다.")
        }

        val userId = jwtTokenProvider.getUserIdFromToken(request.refreshToken)
        val newAccessToken = jwtTokenProvider.generateAccessToken(userId)
        val newRefreshToken = jwtTokenProvider.generateRefreshToken(userId)

        return TokenResponse(
            accessToken = newAccessToken,
            refreshToken = newRefreshToken
        )
    }
}
