package com.mindbridge.oye.controller

import com.mindbridge.oye.config.AppleTokenVerifier
import com.mindbridge.oye.config.JwtTokenProvider
import com.mindbridge.oye.config.KakaoTokenVerifier
import com.mindbridge.oye.controller.api.AuthApi
import com.mindbridge.oye.domain.SocialProvider
import com.mindbridge.oye.domain.Role
import com.mindbridge.oye.dto.AdminKakaoCodeRequest
import com.mindbridge.oye.dto.AdminLoginRequest
import com.mindbridge.oye.dto.AppleLoginRequest
import com.mindbridge.oye.dto.KakaoLoginRequest
import com.mindbridge.oye.dto.RefreshTokenRequest
import com.mindbridge.oye.dto.TokenResponse
import com.mindbridge.oye.exception.ForbiddenException
import com.mindbridge.oye.exception.UnauthorizedException
import com.mindbridge.oye.exception.UserNotFoundException
import com.mindbridge.oye.repository.SocialAccountRepository
import com.mindbridge.oye.repository.UserRepository
import com.mindbridge.oye.service.AuthService
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.RestTemplate
import org.springframework.web.servlet.view.RedirectView

@RestController
@RequestMapping("/api/v1/auth")
class AuthController(
    private val jwtTokenProvider: JwtTokenProvider,
    private val appleTokenVerifier: AppleTokenVerifier,
    private val kakaoTokenVerifier: KakaoTokenVerifier,
    private val userRepository: UserRepository,
    private val socialAccountRepository: SocialAccountRepository,
    private val authService: AuthService,
    @Value("\${spring.security.oauth2.client.registration.kakao.client-id}")
    private val kakaoClientId: String,
    @Value("\${spring.security.oauth2.client.registration.kakao.client-secret}")
    private val kakaoClientSecret: String
) : AuthApi {

    private val log = LoggerFactory.getLogger(javaClass)

    @GetMapping("/login/kakao")
    override fun loginKakao(
        @RequestParam(defaultValue = "web") platform: String,
        @RequestParam(name = "redirect_uri", required = false) redirectUri: String?,
        request: HttpServletRequest
    ): RedirectView {
        val session = request.session
        session.setAttribute("oauth2_platform", platform)
        redirectUri?.let { session.setAttribute("oauth2_redirect_uri", it) }
        return RedirectView("/oauth2/authorization/kakao")
    }

    @PostMapping("/refresh")
    override fun refresh(@RequestBody request: RefreshTokenRequest): TokenResponse {
        if (!jwtTokenProvider.validateToken(request.refreshToken)) {
            throw UnauthorizedException("유효하지 않은 리프레시 토큰입니다.")
        }

        val userId = jwtTokenProvider.getUserIdFromToken(request.refreshToken)
        return TokenResponse(
            accessToken = jwtTokenProvider.generateAccessToken(userId),
            refreshToken = jwtTokenProvider.generateRefreshToken(userId)
        )
    }

    @PostMapping("/login/apple")
    @Transactional
    override fun loginApple(@RequestBody request: AppleLoginRequest): TokenResponse {
        val appleUserId = try {
            appleTokenVerifier.verify(request.identityToken)
        } catch (e: Exception) {
            log.error("Apple 토큰 검증 실패", e)
            throw UnauthorizedException("유효하지 않은 Apple 토큰입니다.")
        }

        val socialAccount = socialAccountRepository.findByProviderAndProviderId(SocialProvider.APPLE, appleUserId)
        val isNewUser = socialAccount == null
        val user = socialAccount?.user ?: authService.createUser(SocialProvider.APPLE, appleUserId, request.fullName)

        return TokenResponse(
            accessToken = jwtTokenProvider.generateAccessToken(user.id!!),
            refreshToken = jwtTokenProvider.generateRefreshToken(user.id!!),
            isNewUser = isNewUser
        )
    }

    @PostMapping("/login/kakao/native")
    @Transactional
    override fun loginKakaoNative(@RequestBody request: KakaoLoginRequest): TokenResponse {
        val kakaoUser = try {
            kakaoTokenVerifier.verify(request.accessToken)
        } catch (e: Exception) {
            log.error("카카오 토큰 검증 실패", e)
            throw UnauthorizedException("유효하지 않은 카카오 토큰입니다.")
        }

        val socialAccount = socialAccountRepository.findByProviderAndProviderId(SocialProvider.KAKAO, kakaoUser.id)
        val isNewUser = socialAccount == null
        val user = socialAccount?.user ?: authService.createUser(SocialProvider.KAKAO, kakaoUser.id, kakaoUser.nickname)

        return TokenResponse(
            accessToken = jwtTokenProvider.generateAccessToken(user.id!!),
            refreshToken = jwtTokenProvider.generateRefreshToken(user.id!!),
            isNewUser = isNewUser
        )
    }

    @PostMapping("/admin/login")
    override fun adminLogin(@RequestBody request: AdminLoginRequest): TokenResponse {
        if (!jwtTokenProvider.validateToken(request.refreshToken)) {
            throw UnauthorizedException("유효하지 않은 리프레시 토큰입니다.")
        }

        val userId = jwtTokenProvider.getUserIdFromToken(request.refreshToken)
        val user = userRepository.findById(userId)
            .orElseThrow { UserNotFoundException() }

        if (user.role != Role.ADMIN) {
            throw ForbiddenException("관리자 권한이 필요합니다.")
        }

        return TokenResponse(
            accessToken = jwtTokenProvider.generateAccessToken(user.id!!),
            refreshToken = jwtTokenProvider.generateRefreshToken(user.id!!)
        )
    }

    @GetMapping("/admin/kakao")
    override fun adminKakaoRedirect(
        @RequestParam("redirect_uri") redirectUri: String
    ): Map<String, String> {
        val url = "https://kauth.kakao.com/oauth/authorize" +
            "?client_id=$kakaoClientId" +
            "&redirect_uri=$redirectUri" +
            "&response_type=code"
        return mapOf("url" to url)
    }

    @PostMapping("/admin/login/kakao")
    @Transactional(readOnly = true)
    override fun adminLoginKakao(@RequestBody request: KakaoLoginRequest): TokenResponse {
        val kakaoUser = try {
            kakaoTokenVerifier.verify(request.accessToken)
        } catch (e: Exception) {
            log.error("관리자 카카오 토큰 검증 실패", e)
            throw UnauthorizedException("유효하지 않은 카카오 토큰입니다.")
        }

        val socialAccount = socialAccountRepository.findByProviderAndProviderId(SocialProvider.KAKAO, kakaoUser.id)
            ?: throw UnauthorizedException("가입되지 않은 사용자입니다.")

        val user = socialAccount.user
        if (user.role != Role.ADMIN) {
            throw ForbiddenException("관리자 권한이 필요합니다.")
        }

        return TokenResponse(
            accessToken = jwtTokenProvider.generateAccessToken(user.id!!),
            refreshToken = jwtTokenProvider.generateRefreshToken(user.id!!)
        )
    }

    @PostMapping("/admin/login/kakao/code")
    @Transactional(readOnly = true)
    override fun adminLoginKakaoCode(@RequestBody request: AdminKakaoCodeRequest): TokenResponse {
        val kakaoAccessToken = exchangeKakaoCode(request.code, request.redirectUri)

        val kakaoUser = try {
            kakaoTokenVerifier.verify(kakaoAccessToken)
        } catch (e: Exception) {
            log.error("관리자 카카오 토큰 검증 실패", e)
            throw UnauthorizedException("유효하지 않은 카카오 토큰입니다.")
        }

        val socialAccount = socialAccountRepository.findByProviderAndProviderId(SocialProvider.KAKAO, kakaoUser.id)
            ?: throw UnauthorizedException("가입되지 않은 사용자입니다.")

        val user = socialAccount.user
        if (user.role != Role.ADMIN) {
            throw ForbiddenException("관리자 권한이 필요합니다.")
        }

        return TokenResponse(
            accessToken = jwtTokenProvider.generateAccessToken(user.id!!),
            refreshToken = jwtTokenProvider.generateRefreshToken(user.id!!)
        )
    }

    private fun exchangeKakaoCode(code: String, redirectUri: String): String {
        val restTemplate = RestTemplate()
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_FORM_URLENCODED

        val body = LinkedMultiValueMap<String, String>()
        body.add("grant_type", "authorization_code")
        body.add("client_id", kakaoClientId)
        body.add("client_secret", kakaoClientSecret)
        body.add("redirect_uri", redirectUri)
        body.add("code", code)

        val response = restTemplate.postForEntity(
            "https://kauth.kakao.com/oauth/token",
            HttpEntity(body, headers),
            Map::class.java
        )

        return response.body?.get("access_token")?.toString()
            ?: throw UnauthorizedException("카카오 토큰 교환에 실패했습니다.")
    }

    @PostMapping("/logout")
    override fun logout(): ResponseEntity<Map<String, String>> {
        return ResponseEntity.ok(mapOf("message" to "로그아웃되었습니다."))
    }

}
