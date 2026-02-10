package com.mindbridge.oye.controller.api

import com.mindbridge.oye.dto.FortuneResponse
import com.mindbridge.oye.exception.ErrorResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "운세", description = "AI 기반 오늘의 운세 조회 API")
interface FortuneApi {

    @Operation(
        summary = "오늘의 운세 조회",
        description = """오늘의 운세를 조회합니다.

- 오늘 이미 생성된 운세가 있으면 캐시된 결과를 반환합니다.
- 없으면 AI가 사용자 정보(이름, 성별, 생년월일, 양력/음력)를 기반으로 새로 생성합니다.
- 하루에 한 번만 생성됩니다."""
    )
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "운세 조회 성공",
            content = [Content(schema = Schema(implementation = FortuneResponse::class))]
        ),
        ApiResponse(
            responseCode = "401",
            description = "인증 실패",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        ApiResponse(
            responseCode = "500",
            description = "AI 운세 생성 실패",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    )
    fun getTodayFortune(principal: Any?): FortuneResponse

    @Operation(
        summary = "운세 히스토리 조회",
        description = "과거에 생성된 운세 목록을 최신순으로 조회합니다."
    )
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "히스토리 조회 성공",
            content = [Content(array = ArraySchema(schema = Schema(implementation = FortuneResponse::class)))]
        ),
        ApiResponse(
            responseCode = "401",
            description = "인증 실패",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    )
    fun getFortuneHistory(principal: Any?): List<FortuneResponse>
}
