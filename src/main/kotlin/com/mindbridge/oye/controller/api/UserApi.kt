package com.mindbridge.oye.controller.api

import com.mindbridge.oye.dto.UserResponse
import com.mindbridge.oye.dto.UserUpdateRequest
import com.mindbridge.oye.exception.ErrorResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "사용자", description = "사용자 정보 조회 및 수정 API")
interface UserApi {

    @Operation(
        summary = "내 정보 조회",
        description = "현재 로그인한 사용자의 프로필 정보를 조회합니다."
    )
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "조회 성공",
            content = [Content(schema = Schema(implementation = UserResponse::class))]
        ),
        ApiResponse(
            responseCode = "401",
            description = "인증 실패 - 토큰이 없거나 유효하지 않음",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        ApiResponse(
            responseCode = "404",
            description = "사용자를 찾을 수 없음",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    )
    fun getMe(principal: Any?): UserResponse

    @Operation(
        summary = "내 정보 수정",
        description = """현재 로그인한 사용자의 프로필 정보를 수정합니다.

- **name**, **birthDate**는 필수 값입니다.
- **gender**, **calendarType**은 선택 값이며, null이면 기존 값을 유지합니다.
- 온보딩 완료 시 생년월일·성별·달력유형을 함께 전송합니다."""
    )
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "수정 성공",
            content = [Content(schema = Schema(implementation = UserResponse::class))]
        ),
        ApiResponse(
            responseCode = "400",
            description = "유효성 검증 실패 (이름 누락, 생년월일이 미래 날짜 등)",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        ApiResponse(
            responseCode = "401",
            description = "인증 실패",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        ApiResponse(
            responseCode = "404",
            description = "사용자를 찾을 수 없음",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    )
    fun updateMe(principal: Any?, request: UserUpdateRequest): UserResponse
}
