package com.mindbridge.oye.controller.api

import com.mindbridge.oye.dto.ApiResponse
import com.mindbridge.oye.dto.LottoRecommendationResponse
import com.mindbridge.oye.dto.LottoRoundResponse
import com.mindbridge.oye.dto.LottoWinnerResponse
import com.mindbridge.oye.dto.PageResponse
import com.mindbridge.oye.exception.ErrorResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "로또", description = "로또 추천 번호 API")
interface LottoApi {

    @Operation(
        summary = "추천 번호 생성",
        description = """로또 추천 번호 5세트를 생성합니다.

- 회차당 1회만 생성 가능합니다.
- round 파라미터를 생략하면 현재 회차를 사용합니다."""
    )
    @ApiResponses(
        io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "추천 번호 생성 성공"
        ),
        io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "인증 실패",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "409",
            description = "해당 회차에 이미 추천 번호가 존재",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    )
    fun recommend(
        principal: Any?,
        @Parameter(description = "회차 번호 (생략 시 현재 회차)", example = "1130") round: Int?
    ): ApiResponse<List<LottoRecommendationResponse>>

    @Operation(
        summary = "내 추천 히스토리 조회",
        description = """내 로또 추천 번호 히스토리를 최신순으로 조회합니다.

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
    fun getMyHistory(
        principal: Any?,
        @Parameter(description = "페이지 번호 (0부터 시작)", example = "0") page: Int,
        @Parameter(description = "페이지 크기", example = "20") size: Int
    ): ApiResponse<PageResponse<LottoRecommendationResponse>>

    @Operation(
        summary = "당첨자 게시판 조회",
        description = """당첨자 게시판을 조회합니다.

- 완전 익명으로 당첨 정보만 표시됩니다.
- 기본값: page=0, size=20"""
    )
    @ApiResponses(
        io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "당첨자 목록 조회 성공"
        )
    )
    fun getWinners(
        @Parameter(description = "페이지 번호 (0부터 시작)", example = "0") page: Int,
        @Parameter(description = "페이지 크기", example = "20") size: Int
    ): ApiResponse<PageResponse<LottoWinnerResponse>>

    @Operation(
        summary = "회차 당첨 번호 조회",
        description = "특정 회차의 당첨 번호를 조회합니다."
    )
    @ApiResponses(
        io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "당첨 번호 조회 성공"
        ),
        io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "해당 회차를 찾을 수 없음",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    )
    fun getRound(
        @Parameter(description = "회차 번호", example = "1130") round: Int
    ): ApiResponse<LottoRoundResponse>
}
