package com.mindbridge.oye.controller.api

import com.mindbridge.oye.dto.ApiResponse
import com.mindbridge.oye.dto.PageResponse
import com.mindbridge.oye.dto.PushNotificationResponse
import com.mindbridge.oye.dto.PushTokenRequest
import com.mindbridge.oye.dto.SendPushRequest
import com.mindbridge.oye.exception.ErrorResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerResponse

@Tag(name = "푸시 알림 (관리자)", description = "관리자 푸시 알림 발송 및 이력 조회 API")
interface PushNotificationAdminApi {

    @Operation(
        summary = "푸시 알림 발송",
        description = """전체 또는 특정 사용자에게 푸시 알림을 발송합니다.
- 관리자만 사용 가능합니다.
- targetType이 SPECIFIC일 때 targetUserIds를 반드시 지정해야 합니다."""
    )
    @ApiResponses(
        SwaggerResponse(
            responseCode = "200",
            description = "발송 성공",
            content = [Content(schema = Schema(implementation = PushNotificationResponse::class))]
        ),
        SwaggerResponse(
            responseCode = "403",
            description = "권한 없음",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    )
    fun sendPush(principal: Any?, request: SendPushRequest): PushNotificationResponse

    @Operation(
        summary = "푸시 알림 발송 이력 조회",
        description = """푸시 알림 발송 이력을 최신순으로 페이지네이션하여 조회합니다.
- 관리자만 사용 가능합니다."""
    )
    @ApiResponses(
        SwaggerResponse(
            responseCode = "200",
            description = "조회 성공"
        ),
        SwaggerResponse(
            responseCode = "403",
            description = "권한 없음",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    )
    fun getPushHistory(
        principal: Any?,
        @Parameter(description = "페이지 번호 (0부터 시작)", example = "0") page: Int,
        @Parameter(description = "페이지 크기", example = "20") size: Int
    ): ApiResponse<PageResponse<PushNotificationResponse>>
}

@Tag(name = "사용자", description = "사용자 정보 조회, 수정, 삭제 API")
interface PushTokenApi {

    @Operation(
        summary = "푸시 토큰 등록/해제",
        description = """현재 로그인한 사용자의 Expo 푸시 토큰을 등록하거나 해제합니다.
- token이 null이면 토큰을 해제합니다 (로그아웃 시 사용)."""
    )
    @ApiResponses(
        SwaggerResponse(
            responseCode = "204",
            description = "토큰 업데이트 성공"
        ),
        SwaggerResponse(
            responseCode = "401",
            description = "인증 실패",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    )
    fun updatePushToken(principal: Any?, request: PushTokenRequest)
}
