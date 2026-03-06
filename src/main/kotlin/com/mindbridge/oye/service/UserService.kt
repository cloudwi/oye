package com.mindbridge.oye.service

import com.mindbridge.oye.domain.SocialProvider
import com.mindbridge.oye.domain.User
import com.mindbridge.oye.dto.UserResponse
import com.mindbridge.oye.dto.UserUpdateRequest
import com.mindbridge.oye.repository.SocialAccountRepository
import com.mindbridge.oye.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class UserService(
    private val userRepository: UserRepository,
    private val socialAccountRepository: SocialAccountRepository
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional(readOnly = true)
    fun getProfile(user: User): UserResponse {
        return UserResponse.from(user, getProvider(user))
    }

    @Transactional
    fun updateProfile(user: User, request: UserUpdateRequest): UserResponse {
        user.name = request.name
        user.birthDate = request.birthDate
        request.birthTime?.let { user.birthTime = it }
        request.gender?.let { user.gender = it }
        request.calendarType?.let { user.calendarType = it }
        request.occupation?.let { user.occupation = it }
        request.mbti?.let { user.mbti = it }
        request.bloodType?.let { user.bloodType = it }
        request.interests?.let { user.interests = it }
        val savedUser = userRepository.save(user)
        return UserResponse.from(savedUser, getProvider(savedUser))
    }

    @Transactional
    fun deleteUser(user: User) {
        log.info("사용자 소프트 딜리트: userId={}", user.id)
        user.deletedAt = LocalDateTime.now()
        user.expoPushToken = null
        user.connectCode = null
        socialAccountRepository.deleteAllByUser(user)
        userRepository.save(user)
        log.info("사용자 소프트 딜리트 완료: userId={}", user.id)
    }

    private fun getProvider(user: User): SocialProvider? {
        return socialAccountRepository.findFirstByUser(user)?.provider
    }
}
