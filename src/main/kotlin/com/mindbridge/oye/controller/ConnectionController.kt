package com.mindbridge.oye.controller

import com.mindbridge.oye.config.AuthenticationResolver
import com.mindbridge.oye.controller.api.ConnectionApi
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
@RequestMapping("/api/v1/connections")
class ConnectionController(
    private val connectionService: ConnectionService,
    private val authenticationResolver: AuthenticationResolver
) : ConnectionApi {

    @GetMapping("/my-code")
    override fun getMyCode(@AuthenticationPrincipal principal: Any?): MyCodeResponse {
        val user = authenticationResolver.getCurrentUser(principal)
        return connectionService.getMyCode(user)
    }

    @PostMapping
    override fun connect(
        @AuthenticationPrincipal principal: Any?,
        @RequestBody request: ConnectRequest
    ): ConnectionResponse {
        val user = authenticationResolver.getCurrentUser(principal)
        return connectionService.connect(user, request)
    }

    @GetMapping
    override fun getMyConnections(@AuthenticationPrincipal principal: Any?): List<ConnectionResponse> {
        val user = authenticationResolver.getCurrentUser(principal)
        return connectionService.getMyConnections(user)
    }

    @DeleteMapping("/{id}")
    override fun deleteConnection(
        @AuthenticationPrincipal principal: Any?,
        @PathVariable id: Long
    ) {
        val user = authenticationResolver.getCurrentUser(principal)
        connectionService.deleteConnection(user, id)
    }
}
