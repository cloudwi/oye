package com.mindbridge.oye.controller

import com.mindbridge.oye.config.AuthenticationResolver
import com.mindbridge.oye.controller.api.ConnectionApi
import com.mindbridge.oye.dto.ApiResponse
import com.mindbridge.oye.dto.ConnectRequest
import com.mindbridge.oye.dto.ConnectionResponse
import com.mindbridge.oye.dto.MyCodeResponse
import com.mindbridge.oye.service.ConnectionService
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/connections")
class ConnectionController(
    private val connectionService: ConnectionService,
    private val authenticationResolver: AuthenticationResolver
) : ConnectionApi {

    @GetMapping("/my-code")
    override fun getMyCode(@AuthenticationPrincipal principal: Any?): ApiResponse<MyCodeResponse> {
        val user = authenticationResolver.getCurrentUser(principal)
        return ApiResponse.success(connectionService.getMyCode(user))
    }

    @PostMapping
    override fun connect(
        @AuthenticationPrincipal principal: Any?,
        @RequestBody request: ConnectRequest
    ): ApiResponse<ConnectionResponse> {
        val user = authenticationResolver.getCurrentUser(principal)
        return ApiResponse.success(connectionService.connect(user, request))
    }

    @GetMapping
    override fun getMyConnections(@AuthenticationPrincipal principal: Any?): ApiResponse<List<ConnectionResponse>> {
        val user = authenticationResolver.getCurrentUser(principal)
        return ApiResponse.success(connectionService.getMyConnections(user))
    }

    @DeleteMapping("/{id}")
    override fun deleteConnection(
        @AuthenticationPrincipal principal: Any?,
        @PathVariable id: Long
    ): ApiResponse<Unit> {
        val user = authenticationResolver.getCurrentUser(principal)
        connectionService.deleteConnection(user, id)
        return ApiResponse.success(Unit)
    }
}
