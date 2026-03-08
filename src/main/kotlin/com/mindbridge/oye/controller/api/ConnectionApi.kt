package com.mindbridge.oye.controller.api

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

    @Operation(summary = "내 초대 코드 조회")
    fun getMyCode(principal: Any?): MyCodeResponse

    @Operation(summary = "친구 요청 보내기", description = "상대방에게 친구 요청을 보냅니다. 상대방이 수락해야 연결이 완료됩니다.")
    fun connect(principal: Any?, request: ConnectRequest): ConnectionResponse

    @Operation(summary = "내 연결 목록 조회", description = "수락된 연결만 조회합니다.")
    fun getMyConnections(principal: Any?): List<ConnectionResponse>

    @Operation(summary = "받은 친구 요청 목록", description = "아직 수락/거절하지 않은 받은 요청을 조회합니다.")
    fun getPendingRequests(principal: Any?): List<ConnectionResponse>

    @Operation(summary = "친구 요청 수락")
    fun acceptConnection(principal: Any?, id: Long): ConnectionResponse

    @Operation(summary = "친구 요청 거절")
    fun rejectConnection(principal: Any?, id: Long)

    @Operation(summary = "연결 삭제")
    fun deleteConnection(principal: Any?, id: Long)

    @Operation(summary = "연인 설정")
    fun setLover(principal: Any?, id: Long): ConnectionResponse

    @Operation(summary = "연인 해제")
    fun unsetLover(principal: Any?, id: Long): ConnectionResponse
}
