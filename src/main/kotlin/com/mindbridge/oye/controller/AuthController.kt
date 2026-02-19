package com.mindbridge.oye.controller

import com.mindbridge.oye.config.AppleTokenVerifier
import com.mindbridge.oye.config.JwtTokenProvider
import com.mindbridge.oye.controller.api.AuthApi
import com.mindbridge.oye.domain.CalendarType
import com.mindbridge.oye.domain.SocialAccount
import com.mindbridge.oye.domain.SocialProvider
import com.mindbridge.oye.domain.User
import com.mindbridge.oye.exception.UnauthorizedException
import com.mindbridge.oye.repository.SocialAccountRepository
import com.mindbridge.oye.repository.UserRepository
import io.swagger.v3.oas.annotations.media.Schema
import org.slf4j.LoggerFactory
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.view.RedirectView
import java.time.LocalDate

@Schema(description = "토큰 갱신 요청")
data class RefreshTokenRequest(
    @Schema(description = "리프레시 토큰", example = "eyJhbGciOiJIUzI1NiJ9...", requiredMode = Schema.RequiredMode.REQUIRED)
    val refreshToken: String
)

@Schema(description = "토큰 응답")
data class TokenResponse(
    @Schema(description = "액세스 토큰 (API 호출 시 Authorization 헤더에 사용)", example = "eyJhbGciOiJIUzI1NiJ9...")
    val accessToken: String,
    @Schema(description = "리프레시 토큰 (액세스 토큰 만료 시 갱신에 사용)", example = "eyJhbGciOiJIUzI1NiJ9...")
    val refreshToken: String
)

@Schema(description = "Apple 로그인 요청")
data class AppleLoginRequest(
    @Schema(description = "Apple identityToken (JWT)", requiredMode = Schema.RequiredMode.REQUIRED)
    val identityToken: String,
    @Schema(description = "사용자 이름 (최초 로그인 시에만 제공)", nullable = true)
    val fullName: String? = null
)

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val jwtTokenProvider: JwtTokenProvider,
    private val appleTokenVerifier: AppleTokenVerifier,
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
        if (redirectUri != null) {
            session.setAttribute("oauth2_redirect_uri", redirectUri)
        }
        return RedirectView("/oauth2/authorization/kakao")
    }

    @PostMapping("/refresh")
    override fun refresh(@RequestBody request: RefreshTokenRequest): TokenResponse {
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

    @PostMapping("/login/apple")
    override fun loginApple(@RequestBody request: AppleLoginRequest): TokenResponse {
        val appleUserId = try {
            appleTokenVerifier.verify(request.identityToken)
        } catch (e: Exception) {
            log.error("Apple 토큰 검증 실패", e)
            throw UnauthorizedException("유효하지 않은 Apple 토큰입니다.")
        }

        val socialAccount = socialAccountRepository.findByProviderAndProviderId(SocialProvider.APPLE, appleUserId)
        val user = socialAccount?.user
            ?: userRepository.save(
                User(
                    name = request.fullName ?: "사용자",
                    birthDate = LocalDate.of(2000, 1, 1),
                    calendarType = CalendarType.SOLAR
                )
            ).also { newUser ->
                socialAccountRepository.save(
                    SocialAccount(
                        user = newUser,
                        provider = SocialProvider.APPLE,
                        providerId = appleUserId
                    )
                )
            }

        val accessToken = jwtTokenProvider.generateAccessToken(user.id!!)
        val refreshToken = jwtTokenProvider.generateRefreshToken(user.id!!)

        return TokenResponse(
            accessToken = accessToken,
            refreshToken = refreshToken
        )
    }

    @PostMapping("/logout")
    override fun logout(): ResponseEntity<Map<String, String>> {
        // Stateless 방식: 서버에서는 별도 처리 없이 성공 응답 반환
        // 클라이언트에서 토큰을 삭제하여 로그아웃 처리
        return ResponseEntity.ok(mapOf("message" to "로그아웃되었습니다."))
    }
}
