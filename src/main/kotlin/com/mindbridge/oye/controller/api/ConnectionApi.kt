package com.mindbridge.oye.controller.api

import com.mindbridge.oye.dto.ApiResponse
import com.mindbridge.oye.dto.ConnectRequest
import com.mindbridge.oye.dto.ConnectionResponse
import com.mindbridge.oye.dto.MyCodeResponse
import com.mindbridge.oye.exception.ErrorResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "연결", description = "사용자 간 연결 (궁합 대상) 관리 API")
interface ConnectionApi {

    @Operation(
        summary = "내 초대 코드 조회",
        description = "내 초대 코드를 조회합니다. 코드가 없으면 새로 생성합니다."
    )
    @ApiResponses(
        io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "초대 코드 조회 성공"
        ),
        io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "인증 실패",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    )
    fun getMyCode(principal: Any?): ApiResponse<MyCodeResponse>

    @Operation(
        summary = "연결 생성",
        description = "상대방의 초대 코드를 사용하여 연결을 생성합니다."
    )
    @ApiResponses(
        io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "연결 생성 성공"
        ),
        io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "자기 자신과 연결 시도",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "인증 실패",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "초대 코드 사용자 없음",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "409",
            description = "이미 연결된 사용자",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    )
    fun connect(principal: Any?, request: ConnectRequest): ApiResponse<ConnectionResponse>

    @Operation(
        summary = "내 연결 목록 조회",
        description = "내가 연결된 모든 사용자 목록을 조회합니다."
    )
    @ApiResponses(
        io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "연결 목록 조회 성공"
        ),
        io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "인증 실패",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    )
    fun getMyConnections(principal: Any?): ApiResponse<List<ConnectionResponse>>

    @Operation(
        summary = "연결 삭제",
        description = "연결을 삭제합니다. 연결에 포함된 궁합 기록도 함께 삭제됩니다."
    )
    @ApiResponses(
        io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "연결 삭제 성공"
        ),
        io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "인증 실패",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            description = "삭제 권한 없음",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "연결을 찾을 수 없음",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    )
    fun deleteConnection(principal: Any?, id: Long): ApiResponse<Unit>
}
