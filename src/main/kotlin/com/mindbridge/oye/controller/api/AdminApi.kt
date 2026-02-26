package com.mindbridge.oye.controller.api

import com.mindbridge.oye.dto.AdminDashboardStats
import com.mindbridge.oye.dto.AdminUserResponse
import com.mindbridge.oye.dto.ApiResponse
import com.mindbridge.oye.dto.PageResponse
import com.mindbridge.oye.dto.RoleUpdateRequest
import com.mindbridge.oye.exception.ErrorResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerResponse

@Tag(name = "관리자", description = "관리자 대시보드 API")
interface AdminApi {

    @Operation(
        summary = "대시보드 통계 조회",
        description = """관리자 대시보드 통계를 조회합니다.
- 전체 사용자 수, 전체 문의 수, 미답변 문의 수를 반환합니다.
- 관리자만 사용 가능합니다."""
    )
    @ApiResponses(
        SwaggerResponse(
            responseCode = "200",
            description = "조회 성공",
            content = [Content(schema = Schema(implementation = AdminDashboardStats::class))]
        ),
        SwaggerResponse(
            responseCode = "403",
            description = "권한 없음",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    )
    fun getStats(principal: Any?): AdminDashboardStats

    @Operation(
        summary = "사용자 목록 조회",
        description = """전체 사용자 목록을 최신 가입순으로 페이지네이션하여 조회합니다.
- 관리자만 사용 가능합니다.
- search 파라미터로 이름 검색이 가능합니다."""
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
    fun getUsers(
        principal: Any?,
        @Parameter(description = "페이지 번호 (0부터 시작)", example = "0") page: Int,
        @Parameter(description = "페이지 크기", example = "20") size: Int,
        @Parameter(description = "이름 검색어", required = false) search: String?
    ): ApiResponse<PageResponse<AdminUserResponse>>

    @Operation(
        summary = "사용자 권한 변경",
        description = """사용자의 권한을 변경합니다.
- 관리자만 사용 가능합니다.
- USER 또는 ADMIN으로 변경할 수 있습니다."""
    )
    @ApiResponses(
        SwaggerResponse(
            responseCode = "200",
            description = "변경 성공",
            content = [Content(schema = Schema(implementation = AdminUserResponse::class))]
        ),
        SwaggerResponse(
            responseCode = "403",
            description = "권한 없음",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        SwaggerResponse(
            responseCode = "404",
            description = "사용자를 찾을 수 없음",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    )
    fun updateUserRole(
        principal: Any?,
        @Parameter(description = "사용자 ID", example = "1") id: Long,
        request: RoleUpdateRequest
    ): AdminUserResponse
}
