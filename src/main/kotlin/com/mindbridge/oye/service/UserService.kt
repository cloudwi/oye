package com.mindbridge.oye.service

import com.mindbridge.oye.domain.SocialProvider
import com.mindbridge.oye.domain.User
import com.mindbridge.oye.dto.UserResponse
import com.mindbridge.oye.dto.UserUpdateRequest
import com.mindbridge.oye.repository.FortuneRepository
import com.mindbridge.oye.repository.InquiryRepository
import com.mindbridge.oye.repository.SocialAccountRepository
import com.mindbridge.oye.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserService(
    private val userRepository: UserRepository,
    private val fortuneRepository: FortuneRepository,
    private val socialAccountRepository: SocialAccountRepository,
    private val inquiryRepository: InquiryRepository
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
        request.gender?.let { user.gender = it }
        request.calendarType?.let { user.calendarType = it }
        val savedUser = userRepository.save(user)
        return UserResponse.from(savedUser, getProvider(savedUser))
    }

    @Transactional
    fun deleteUser(user: User) {
        log.info("사용자 삭제 시작: userId={}", user.id)
        inquiryRepository.deleteAllByUser(user)
        fortuneRepository.deleteAllByUser(user)
        socialAccountRepository.deleteAllByUser(user)
        userRepository.delete(user)
        log.info("사용자 삭제 완료: userId={}", user.id)
    }

    private fun getProvider(user: User): SocialProvider? {
        return socialAccountRepository.findFirstByUser(user)?.provider
    }
}
