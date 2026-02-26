package com.mindbridge.oye.controller

import com.mindbridge.oye.config.AppleTokenVerifier
import com.mindbridge.oye.config.JwtTokenProvider
import com.mindbridge.oye.config.KakaoTokenVerifier
import com.mindbridge.oye.controller.api.AuthApi
import com.mindbridge.oye.domain.CalendarType
import com.mindbridge.oye.domain.SocialAccount
import com.mindbridge.oye.domain.SocialProvider
import com.mindbridge.oye.domain.User
import com.mindbridge.oye.domain.Role
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
import org.springframework.web.servlet.view.RedirectView
import java.time.LocalDate

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val jwtTokenProvider: JwtTokenProvider,
    private val appleTokenVerifier: AppleTokenVerifier,
    private val kakaoTokenVerifier: KakaoTokenVerifier,
    private val userRepository: UserRepository,
    private val socialAccountRepository: SocialAccountRepository
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
        val user = socialAccount?.user ?: createAppleUser(appleUserId, request.fullName)

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
        val user = socialAccount?.user ?: createKakaoUser(kakaoUser.id, kakaoUser.nickname)

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

    @PostMapping("/logout")
    override fun logout(): ResponseEntity<Map<String, String>> {
        return ResponseEntity.ok(mapOf("message" to "로그아웃되었습니다."))
    }

    private fun createKakaoUser(kakaoId: String, nickname: String): User {
        val user = userRepository.save(
            User(
                name = nickname,
                birthDate = LocalDate.of(2000, 1, 1),
                calendarType = CalendarType.SOLAR
            )
        )
        socialAccountRepository.save(
            SocialAccount(
                user = user,
                provider = SocialProvider.KAKAO,
                providerId = kakaoId
            )
        )
        return user
    }

    private fun createAppleUser(appleUserId: String, fullName: String?): User {
        val user = userRepository.save(
            User(
                name = fullName ?: "사용자",
                birthDate = LocalDate.of(2000, 1, 1),
                calendarType = CalendarType.SOLAR
            )
        )
        socialAccountRepository.save(
            SocialAccount(
                user = user,
                provider = SocialProvider.APPLE,
                providerId = appleUserId
            )
        )
        return user
    }
}
