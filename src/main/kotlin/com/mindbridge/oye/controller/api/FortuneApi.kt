package com.mindbridge.oye.controller.api

import com.mindbridge.oye.dto.ApiResponse
import com.mindbridge.oye.dto.FortuneResponse
import com.mindbridge.oye.dto.PageResponse
import com.mindbridge.oye.exception.ErrorResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "예감", description = "AI 기반 오늘의 예감 조회 API")
interface FortuneApi {

    @Operation(
        summary = "오늘의 예감 조회",
        description = """오늘의 예감을 조회합니다.

- 오늘 이미 생성된 예감이 있으면 캐시된 결과를 반환합니다.
- 없으면 AI가 사용자 정보(이름, 성별, 생년월일, 양력/음력)를 기반으로 새로 생성합니다.
- 하루에 한 번만 생성됩니다."""
    )
    @ApiResponses(
        io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "예감 조회 성공",
            content = [Content(schema = Schema(implementation = FortuneResponse::class))]
        ),
        io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "인증 실패",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "500",
            description = "AI 예감 생성 실패",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    )
    fun getTodayFortune(principal: Any?): FortuneResponse

    @Operation(
        summary = "예감 히스토리 조회",
        description = """과거에 생성된 예감 목록을 최신순으로 페이지네이션하여 조회합니다.

- 기본값: page=0, size=20"""
    )
    @ApiResponses(
        io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "히스토리 조회 성공"
        ),
        io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "인증 실패",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    )
    fun getFortuneHistory(
        principal: Any?,
        @Parameter(description = "페이지 번호 (0부터 시작)", example = "0") page: Int,
        @Parameter(description = "페이지 크기", example = "20") size: Int
    ): ApiResponse<PageResponse<FortuneResponse>>
}
