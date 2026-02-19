package com.mindbridge.oye.config

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.security.web.authentication.AuthenticationSuccessHandler
import org.springframework.stereotype.Component

@Component
class OAuth2SuccessHandler(
    private val jwtTokenProvider: JwtTokenProvider,
    @org.springframework.beans.factory.annotation.Value("\${app.web-redirect-uri:/}")
    private val defaultWebRedirectUri: String
) : AuthenticationSuccessHandler {

    override fun onAuthenticationSuccess(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authentication: Authentication
    ) {
        val oAuth2User = authentication.principal as OAuth2User
        val userId = oAuth2User.attributes["userId"] as Long

        val accessToken = jwtTokenProvider.generateAccessToken(userId)
        val refreshToken = jwtTokenProvider.generateRefreshToken(userId)

        val session = request.session
        val platform = session.getAttribute("oauth2_platform") as? String ?: "web"
        val redirectUri = session.getAttribute("oauth2_redirect_uri") as? String

        session.removeAttribute("oauth2_platform")
        session.removeAttribute("oauth2_redirect_uri")

        val fragment = "token=$accessToken&refresh_token=$refreshToken"
        val targetUrl = if (platform == "native") {
            "oyeapp://auth/callback#$fragment"
        } else {
            val baseUri = redirectUri ?: defaultWebRedirectUri
            "$baseUri#$fragment"
        }

        response.sendRedirect(targetUrl)
    }
}
