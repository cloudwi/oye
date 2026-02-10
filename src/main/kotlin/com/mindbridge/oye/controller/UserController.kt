package com.mindbridge.oye.controller

import com.mindbridge.oye.controller.api.UserApi
import com.mindbridge.oye.dto.UserResponse
import com.mindbridge.oye.dto.UserUpdateRequest
import com.mindbridge.oye.exception.UnauthorizedException
import com.mindbridge.oye.exception.UserNotFoundException
import com.mindbridge.oye.repository.UserRepository
import jakarta.validation.Valid
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/users")
class UserController(
    private val userRepository: UserRepository
) : UserApi {

    @GetMapping("/me")
    override fun getMe(@AuthenticationPrincipal principal: Any?): UserResponse {
        val user = getCurrentUser(principal)
        return UserResponse.from(user)
    }

    @PutMapping("/me")
    @Transactional
    override fun updateMe(
        @AuthenticationPrincipal principal: Any?,
        @Valid @RequestBody request: UserUpdateRequest
    ): UserResponse {
        val user = getCurrentUser(principal)
        user.name = request.name
        user.birthDate = request.birthDate
        request.gender?.let { user.gender = it }
        request.calendarType?.let { user.calendarType = it }
        return UserResponse.from(userRepository.save(user))
    }

    private fun getCurrentUser(principal: Any?) = when (principal) {
        is Long -> userRepository.findById(principal).orElseThrow { UserNotFoundException() }
        is OAuth2User -> {
            val userId = principal.attributes["userId"] as? Long
                ?: throw UnauthorizedException("사용자 ID를 찾을 수 없습니다.")
            userRepository.findById(userId).orElseThrow { UserNotFoundException() }
        }
        else -> throw UnauthorizedException()
    }
}
