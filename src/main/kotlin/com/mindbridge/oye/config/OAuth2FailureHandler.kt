package com.mindbridge.oye.config

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.authentication.AuthenticationFailureHandler
import org.springframework.stereotype.Component
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Component
class OAuth2FailureHandler(
    @Value("\${app.web-redirect-uri:/}")
    private val defaultWebRedirectUri: String
) : AuthenticationFailureHandler {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun onAuthenticationFailure(
        request: HttpServletRequest,
        response: HttpServletResponse,
        exception: AuthenticationException
    ) {
        log.error("OAuth2 로그인 실패: {}", exception.message, exception)

        val session = request.getSession(false)
        val platform = session?.getAttribute("oauth2_platform") as? String ?: "web"
        val redirectUri = session?.getAttribute("oauth2_redirect_uri") as? String

        session?.removeAttribute("oauth2_platform")
        session?.removeAttribute("oauth2_redirect_uri")

        val errorMessage = URLEncoder.encode(exception.message ?: "login_failed", StandardCharsets.UTF_8)
        val targetUrl = if (platform == "native") {
            "oyeapp://auth/callback?error=$errorMessage"
        } else {
            val baseUri = redirectUri ?: defaultWebRedirectUri
            "$baseUri?error=$errorMessage"
        }

        response.sendRedirect(targetUrl)
    }
}
