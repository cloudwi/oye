package com.mindbridge.oye.service

import com.mindbridge.oye.domain.User
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

    @Transactional
    fun deleteUser(user: User) {
        log.info("사용자 삭제 시작: userId={}", user.id)
        inquiryRepository.deleteAllByUser(user)
        fortuneRepository.deleteAllByUser(user)
        socialAccountRepository.deleteAllByUser(user)
        userRepository.delete(user)
        log.info("사용자 삭제 완료: userId={}", user.id)
    }
}
