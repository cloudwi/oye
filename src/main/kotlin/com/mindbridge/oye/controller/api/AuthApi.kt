package com.mindbridge.oye.controller.api

import com.mindbridge.oye.controller.RefreshTokenRequest
import com.mindbridge.oye.controller.TokenResponse
import com.mindbridge.oye.exception.ErrorResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import org.springframework.web.servlet.view.RedirectView

@Tag(name = "인증", description = "카카오 OAuth2 로그인 및 JWT 토큰 관리 API")
interface AuthApi {

    @Operation(
        summary = "카카오 로그인",
        description = """카카오 OAuth2 로그인 페이지로 리다이렉트합니다.

- 로그인 성공 시 JWT 토큰이 발급됩니다.
- platform 파라미터로 웹/앱 구분이 가능합니다."""
    )
    @ApiResponses(
        ApiResponse(responseCode = "302", description = "카카오 로그인 페이지로 리다이렉트")
    )
    fun loginKakao(
        @Parameter(description = "플랫폼 (web 또는 app)", example = "app")
        platform: String,
        @Parameter(description = "로그인 완료 후 리다이렉트할 URI", required = false)
        redirectUri: String?,
        request: HttpServletRequest
    ): RedirectView

    @Operation(
        summary = "토큰 갱신",
        description = """리프레시 토큰으로 새로운 액세스 토큰과 리프레시 토큰을 발급받습니다.

- 액세스 토큰 만료 시 이 API를 호출하여 갱신합니다.
- 리프레시 토큰도 함께 갱신되므로, 응답의 새 리프레시 토큰을 저장해야 합니다."""
    )
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "토큰 갱신 성공",
            content = [Content(schema = Schema(implementation = TokenResponse::class))]
        ),
        ApiResponse(
            responseCode = "401",
            description = "리프레시 토큰이 유효하지 않음",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    )
    fun refresh(request: RefreshTokenRequest): TokenResponse
}
