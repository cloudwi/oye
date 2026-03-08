package com.mindbridge.oye.service

import com.mindbridge.oye.domain.SocialProvider
import com.mindbridge.oye.domain.User
import com.mindbridge.oye.dto.UserResponse
import com.mindbridge.oye.dto.UserUpdateRequest
import com.mindbridge.oye.exception.NicknameDuplicateException
import com.mindbridge.oye.exception.NicknameInvalidException
import com.mindbridge.oye.repository.GroupMemberRepository
import com.mindbridge.oye.repository.SocialAccountRepository
import com.mindbridge.oye.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class UserService(
    private val userRepository: UserRepository,
    private val socialAccountRepository: SocialAccountRepository,
    private val groupMemberRepository: GroupMemberRepository,
    private val groupService: GroupService
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

        val memberships = groupMemberRepository.findAllByUser(user)
        for (membership in memberships) {
            groupService.leaveGroup(user, membership.group.id!!)
        }

        user.deletedAt = LocalDateTime.now()
        user.expoPushToken = null
        user.connectCode = null
        user.nickname = null
        socialAccountRepository.deleteAllByUser(user)
        userRepository.save(user)
        log.info("사용자 소프트 딜리트 완료: userId={}", user.id)
    }

    companion object {
        private val NICKNAME_REGEX = Regex("^[가-힣a-zA-Z0-9_]{2,20}$")
    }

    private fun validateNickname(nickname: String) {
        if (!NICKNAME_REGEX.matches(nickname)) {
            throw NicknameInvalidException()
        }
    }

    @Transactional
    fun setNickname(user: User, nickname: String): UserResponse {
        validateNickname(nickname)
        if (user.nickname != nickname && userRepository.existsByNickname(nickname)) {
            throw NicknameDuplicateException()
        }
        user.nickname = nickname
        val saved = userRepository.save(user)
        return UserResponse.from(saved, getProvider(saved))
    }

    @Transactional(readOnly = true)
    fun checkNicknameAvailable(nickname: String): Boolean {
        validateNickname(nickname)
        return !userRepository.existsByNickname(nickname)
    }

    private fun getProvider(user: User): SocialProvider? {
        return socialAccountRepository.findFirstByUser(user)?.provider
    }
}
