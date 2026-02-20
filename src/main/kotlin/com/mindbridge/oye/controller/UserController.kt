package com.mindbridge.oye.controller

import com.mindbridge.oye.config.AuthenticationResolver
import com.mindbridge.oye.controller.api.UserApi
import com.mindbridge.oye.dto.UserResponse
import com.mindbridge.oye.dto.UserUpdateRequest
import com.mindbridge.oye.service.UserService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/users")
class UserController(
    private val userService: UserService,
    private val authenticationResolver: AuthenticationResolver
) : UserApi {

    @GetMapping("/me")
    override fun getMe(@AuthenticationPrincipal principal: Any?): UserResponse {
        val user = authenticationResolver.getCurrentUser(principal)
        return userService.getProfile(user)
    }

    @PutMapping("/me")
    override fun updateMe(
        @AuthenticationPrincipal principal: Any?,
        @Valid @RequestBody request: UserUpdateRequest
    ): UserResponse {
        val user = authenticationResolver.getCurrentUser(principal)
        return userService.updateProfile(user, request)
    }

    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    override fun deleteMe(@AuthenticationPrincipal principal: Any?) {
        val user = authenticationResolver.getCurrentUser(principal)
        userService.deleteUser(user)
    }
}
