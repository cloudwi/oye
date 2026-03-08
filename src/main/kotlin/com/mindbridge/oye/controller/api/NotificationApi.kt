package com.mindbridge.oye.controller.api

import com.mindbridge.oye.dto.ApiResponse
import com.mindbridge.oye.dto.PageResponse
import com.mindbridge.oye.dto.UnreadCountResponse
import com.mindbridge.oye.dto.UserNotificationResponse
import com.mindbridge.oye.exception.ErrorResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerResponse

@Tag(name = "알림", description = "사용자 알림 센터 API")
interface NotificationApi {

    @Operation(
        summary = "알림 목록 조회",
        description = """내 알림 목록을 최신순으로 페이지네이션하여 조회합니다.
- 기본값: page=0, size=20"""
    )
    @ApiResponses(
        SwaggerResponse(
            responseCode = "200",
            description = "조회 성공"
        ),
        SwaggerResponse(
            responseCode = "401",
            description = "인증 실패",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    )
    fun getNotifications(
        principal: Any?,
        @Parameter(description = "페이지 번호 (0부터 시작)", example = "0") page: Int,
        @Parameter(description = "페이지 크기", example = "20") size: Int
    ): ApiResponse<PageResponse<UserNotificationResponse>>

    @Operation(
        summary = "읽지 않은 알림 수 조회",
        description = "읽지 않은 알림의 총 개수를 반환합니다."
    )
    @ApiResponses(
        SwaggerResponse(
            responseCode = "200",
            description = "조회 성공"
        ),
        SwaggerResponse(
            responseCode = "401",
            description = "인증 실패",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    )
    fun getUnreadCount(principal: Any?): ApiResponse<UnreadCountResponse>

    @Operation(
        summary = "알림 읽음 처리",
        description = "특정 알림을 읽음 상태로 변경합니다."
    )
    @ApiResponses(
        SwaggerResponse(
            responseCode = "200",
            description = "읽음 처리 성공"
        ),
        SwaggerResponse(
            responseCode = "401",
            description = "인증 실패",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        SwaggerResponse(
            responseCode = "404",
            description = "알림을 찾을 수 없음",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    )
    fun markAsRead(principal: Any?, @Parameter(description = "알림 ID", example = "1") id: Long): ApiResponse<Any?>

    @Operation(
        summary = "전체 알림 읽음 처리",
        description = "모든 읽지 않은 알림을 읽음 상태로 변경합니다."
    )
    @ApiResponses(
        SwaggerResponse(
            responseCode = "200",
            description = "전체 읽음 처리 성공"
        ),
        SwaggerResponse(
            responseCode = "401",
            description = "인증 실패",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    )
    fun markAllAsRead(principal: Any?): ApiResponse<Any?>
}
