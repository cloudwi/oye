package com.mindbridge.oye.dto

import io.swagger.v3.oas.annotations.media.Schema

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
    val refreshToken: String,
    @Schema(description = "신규 회원 여부 (true: 첫 로그인, false: 기존 회원)", example = "true")
    val isNewUser: Boolean = false
)

@Schema(description = "Apple 로그인 요청")
data class AppleLoginRequest(
    @Schema(description = "Apple identityToken (JWT)", requiredMode = Schema.RequiredMode.REQUIRED)
    val identityToken: String,
    @Schema(description = "사용자 이름 (최초 로그인 시에만 제공)", nullable = true)
    val fullName: String? = null
)
