package com.mindbridge.oye.service

import com.mindbridge.oye.domain.CalendarType
import com.mindbridge.oye.domain.SocialAccount
import com.mindbridge.oye.domain.SocialProvider
import com.mindbridge.oye.domain.User
import com.mindbridge.oye.event.UserCreatedEvent
import com.mindbridge.oye.repository.SocialAccountRepository
import com.mindbridge.oye.repository.UserRepository
import com.mindbridge.oye.util.NicknameGenerator
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val socialAccountRepository: SocialAccountRepository,
    private val eventPublisher: ApplicationEventPublisher
) {
    @Transactional
    fun createUser(provider: SocialProvider, providerId: String, name: String?): User {
        val nickname = generateUniqueNickname()
        val user = userRepository.save(
            User(
                name = name?.ifBlank { null },
                birthDate = LocalDate.of(2000, 1, 1),
                calendarType = CalendarType.SOLAR,
                nickname = nickname
            )
        )
        socialAccountRepository.save(
            SocialAccount(
                user = user,
                provider = provider,
                providerId = providerId
            )
        )
        eventPublisher.publishEvent(UserCreatedEvent(user))
        return user
    }

    private fun generateUniqueNickname(): String {
        repeat(20) {
            val nickname = NicknameGenerator.generate()
            if (!userRepository.existsByNickname(nickname)) {
                return nickname
            }
        }
        return "user_${System.currentTimeMillis()}"
    }
}
