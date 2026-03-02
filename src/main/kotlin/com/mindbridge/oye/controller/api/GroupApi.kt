package com.mindbridge.oye.controller.api

import com.mindbridge.oye.dto.ApiResponse
import com.mindbridge.oye.dto.CreateGroupRequest
import com.mindbridge.oye.dto.GroupCompatibilityResponse
import com.mindbridge.oye.dto.GroupDetailResponse
import com.mindbridge.oye.dto.GroupSummaryResponse
import com.mindbridge.oye.dto.GroupTodayCompatibilityResponse
import com.mindbridge.oye.dto.JoinGroupRequest
import com.mindbridge.oye.dto.UpdateGroupRequest
import com.mindbridge.oye.exception.ErrorResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "그룹", description = "그룹 궁합 관리 API")
interface GroupApi {

    @Operation(
        summary = "그룹 생성",
        description = "새 그룹을 생성합니다. 생성자는 자동으로 멤버로 추가됩니다. 연인 관계 유형은 지원하지 않습니다."
    )
    @ApiResponses(
        io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "그룹 생성 성공"
        ),
        io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "잘못된 관계 유형",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "인증 실패",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    )
    fun createGroup(principal: Any?, request: CreateGroupRequest): ApiResponse<GroupSummaryResponse>

    @Operation(
        summary = "그룹 참여",
        description = "초대 코드를 사용하여 그룹에 참여합니다. 최대 10명까지 참여 가능합니다."
    )
    @ApiResponses(
        io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "그룹 참여 성공"
        ),
        io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "그룹 인원 초과",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "인증 실패",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "그룹을 찾을 수 없음",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "409",
            description = "이미 그룹 멤버",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    )
    fun joinGroup(principal: Any?, request: JoinGroupRequest): ApiResponse<GroupSummaryResponse>

    @Operation(
        summary = "내 그룹 목록 조회",
        description = "내가 참여 중인 모든 그룹 목록을 조회합니다."
    )
    @ApiResponses(
        io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "그룹 목록 조회 성공"
        ),
        io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "인증 실패",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    )
    fun getMyGroups(principal: Any?): ApiResponse<List<GroupSummaryResponse>>

    @Operation(
        summary = "그룹 상세 조회",
        description = "그룹의 상세 정보와 멤버 목록을 조회합니다."
    )
    @ApiResponses(
        io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "그룹 상세 조회 성공"
        ),
        io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "인증 실패",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            description = "그룹 멤버가 아님",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "그룹을 찾을 수 없음",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    )
    fun getGroupDetail(principal: Any?, id: Long): ApiResponse<GroupDetailResponse>

    @Operation(
        summary = "그룹 이름 수정",
        description = "그룹 이름을 수정합니다. 방장만 가능합니다."
    )
    @ApiResponses(
        io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "그룹 수정 성공"
        ),
        io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "인증 실패",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            description = "방장이 아님",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "그룹을 찾을 수 없음",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    )
    fun updateGroup(principal: Any?, id: Long, request: UpdateGroupRequest): ApiResponse<GroupDetailResponse>

    @Operation(
        summary = "그룹 삭제",
        description = "그룹을 삭제합니다. 방장만 가능하며, 모든 멤버와 궁합 기록이 함께 삭제됩니다."
    )
    @ApiResponses(
        io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "그룹 삭제 성공"
        ),
        io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "인증 실패",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            description = "방장이 아님",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "그룹을 찾을 수 없음",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    )
    fun deleteGroup(principal: Any?, id: Long)

    @Operation(
        summary = "그룹 탈퇴",
        description = "그룹에서 탈퇴합니다. 방장이면 다음 멤버에게 방장이 위임되며, 마지막 멤버라면 그룹이 삭제됩니다."
    )
    @ApiResponses(
        io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "그룹 탈퇴 성공"
        ),
        io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "인증 실패",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            description = "그룹 멤버가 아님",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "그룹을 찾을 수 없음",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    )
    fun leaveGroup(principal: Any?, id: Long)

    @Operation(
        summary = "그룹 멤버 추방",
        description = "그룹에서 특정 멤버를 추방합니다. 방장만 가능하며, 자기 자신은 추방할 수 없습니다."
    )
    @ApiResponses(
        io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "멤버 추방 성공"
        ),
        io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "자기 자신 추방 시도",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "인증 실패",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            description = "방장이 아님",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "그룹 또는 멤버를 찾을 수 없음",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    )
    fun kickMember(principal: Any?, id: Long, userId: Long)

    @Operation(
        summary = "그룹 오늘의 궁합 조회",
        description = "그룹 내 모든 멤버 쌍의 오늘 궁합을 조회합니다."
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
            description = "그룹 멤버가 아님",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "그룹을 찾을 수 없음",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    )
    fun getGroupTodayCompatibility(principal: Any?, id: Long): ApiResponse<GroupTodayCompatibilityResponse>

    @Operation(
        summary = "그룹 특정 쌍 궁합 조회",
        description = "그룹 내 나와 특정 멤버의 오늘 궁합을 조회합니다."
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
            description = "그룹 멤버가 아님",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "그룹 또는 궁합을 찾을 수 없음",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    )
    fun getGroupPairCompatibility(principal: Any?, id: Long, userId: Long): ApiResponse<GroupCompatibilityResponse>
}
