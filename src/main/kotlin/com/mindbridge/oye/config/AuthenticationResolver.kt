package com.mindbridge.oye.config

import com.mindbridge.oye.domain.User
import com.mindbridge.oye.exception.UnauthorizedException
import com.mindbridge.oye.exception.UserNotFoundException
import com.mindbridge.oye.repository.UserRepository
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.stereotype.Component

@Component
class AuthenticationResolver(
    private val userRepository: UserRepository
) {
    fun getCurrentUser(principal: Any?): User = when (principal) {
        is Long -> userRepository.findById(principal).orElseThrow { UserNotFoundException() }
        is OAuth2User -> {
            val userId = principal.attributes["userId"] as? Long
                ?: throw UnauthorizedException("사용자 ID를 찾을 수 없습니다.")
            userRepository.findById(userId).orElseThrow { UserNotFoundException() }
        }
        else -> throw UnauthorizedException()
    }
}
