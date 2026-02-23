package com.mindbridge.oye.controller.api

import com.mindbridge.oye.dto.ApiResponse
import com.mindbridge.oye.dto.InquiryCommentCreateRequest
import com.mindbridge.oye.dto.InquiryCreateRequest
import com.mindbridge.oye.dto.InquiryResponse
import com.mindbridge.oye.dto.PageResponse
import com.mindbridge.oye.exception.ErrorResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerResponse

@Tag(name = "문의", description = "문의하기 API")
interface InquiryApi {

    @Operation(
        summary = "문의 작성",
        description = "새로운 문의를 작성합니다."
    )
    @ApiResponses(
        SwaggerResponse(
            responseCode = "200",
            description = "작성 성공",
            content = [Content(schema = Schema(implementation = InquiryResponse::class))]
        ),
        SwaggerResponse(
            responseCode = "401",
            description = "인증 실패",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    )
    fun createInquiry(principal: Any?, request: InquiryCreateRequest): InquiryResponse

    @Operation(
        summary = "내 문의 목록 조회",
        description = """내가 작성한 문의 목록을 최신순으로 페이지네이션하여 조회합니다.
- 기본값: page=0, size=20"""
    )
    fun getMyInquiries(
        principal: Any?,
        @Parameter(description = "페이지 번호 (0부터 시작)", example = "0") page: Int,
        @Parameter(description = "페이지 크기", example = "20") size: Int
    ): ApiResponse<PageResponse<InquiryResponse>>

    @Operation(
        summary = "문의 상세 조회",
        description = """문의 상세 정보를 조회합니다.
- 본인의 문의 또는 관리자만 조회 가능합니다."""
    )
    fun getInquiry(principal: Any?, id: Long): InquiryResponse

    @Operation(
        summary = "문의 댓글 작성",
        description = """관리자가 문의에 댓글을 작성합니다.
- 관리자만 사용 가능합니다.
- 첫 댓글 작성 시 문의 상태가 ANSWERED로 변경됩니다."""
    )
    @ApiResponses(
        SwaggerResponse(
            responseCode = "200",
            description = "댓글 작성 성공",
            content = [Content(schema = Schema(implementation = InquiryResponse::class))]
        ),
        SwaggerResponse(
            responseCode = "403",
            description = "권한 없음",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        SwaggerResponse(
            responseCode = "404",
            description = "문의를 찾을 수 없음",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    )
    fun addComment(principal: Any?, id: Long, request: InquiryCommentCreateRequest): InquiryResponse
}
