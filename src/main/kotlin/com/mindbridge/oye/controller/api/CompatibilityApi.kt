package com.mindbridge.oye.controller.api

import com.mindbridge.oye.dto.ApiResponse
import com.mindbridge.oye.dto.CompatibilityResponse
import com.mindbridge.oye.dto.PageResponse
import com.mindbridge.oye.dto.RecordDatesResponse
import com.mindbridge.oye.dto.ScoreTrendPoint
import com.mindbridge.oye.exception.ErrorResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "궁합", description = "AI 기반 궁합 분석 API")
interface CompatibilityApi {

    @Operation(
        summary = "오늘의 궁합 조회",
        description = """연결된 상대방과의 오늘의 궁합을 조회합니다.

- 오늘 이미 생성된 궁합이 있으면 캐시된 결과를 반환합니다.
- 없으면 AI가 두 사람의 프로필을 기반으로 새로 생성합니다.
- 하루에 한 번만 생성됩니다."""
    )
    @ApiResponses(
        io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "궁합 조회 성공"
        ),
        io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "인증 실패",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            description = "접근 권한 없음",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "연결을 찾을 수 없음",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "500",
            description = "AI 궁합 생성 실패",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    )
    fun getCompatibility(principal: Any?, id: Long): ApiResponse<CompatibilityResponse>

    @Operation(
        summary = "궁합 히스토리 조회",
        description = """과거에 생성된 궁합 결과 목록을 최신순으로 페이지네이션하여 조회합니다.

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
        ),
        io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            description = "접근 권한 없음",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "연결을 찾을 수 없음",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    )
    fun getCompatibilityHistory(
        principal: Any?,
        id: Long,
        @Parameter(description = "페이지 번호 (0부터 시작)", example = "0") page: Int,
        @Parameter(description = "페이지 크기", example = "20") size: Int
    ): ApiResponse<PageResponse<CompatibilityResponse>>

    @Operation(
        summary = "궁합 점수 추이 조회",
        description = "최근 N일간의 궁합 점수 추이를 조회합니다."
    )
    @ApiResponses(
        io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "점수 추이 조회 성공"
        ),
        io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "인증 실패",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            description = "접근 권한 없음",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "연결을 찾을 수 없음",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    )
    fun getScoreTrend(
        principal: Any?,
        id: Long,
        @Parameter(description = "조회 기간 (일)", example = "30") days: Int
    ): ApiResponse<List<ScoreTrendPoint>>

    @Operation(
        summary = "궁합 기록 날짜 조회",
        description = "특정 월에 궁합 기록이 있는 날짜 목록을 조회합니다."
    )
    @ApiResponses(
        io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "기록 날짜 조회 성공"
        ),
        io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "인증 실패",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            description = "접근 권한 없음",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "연결을 찾을 수 없음",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    )
    fun getRecordDates(
        principal: Any?,
        id: Long,
        @Parameter(description = "연도", example = "2026") year: Int,
        @Parameter(description = "월", example = "3") month: Int
    ): ApiResponse<RecordDatesResponse>
}
