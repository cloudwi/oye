package com.mindbridge.oye.event

import com.mindbridge.oye.service.CompatibilityService
import com.mindbridge.oye.service.ConnectionService
import com.mindbridge.oye.service.FortuneService
import com.mindbridge.oye.service.GroupCompatibilityService
import com.mindbridge.oye.service.LottoService
import com.mindbridge.oye.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

@Component
class FortuneEventListener(
    private val fortuneService: FortuneService,
    private val compatibilityService: CompatibilityService,
    private val groupCompatibilityService: GroupCompatibilityService,
    private val connectionService: ConnectionService,
    private val lottoService: LottoService,
    private val userRepository: UserRepository
) {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private val KST = ZoneId.of("Asia/Seoul")
        private const val NEXT_DAY_CUTOFF_HOUR = 21
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun handleUserCreated(event: UserCreatedEvent) {
        val user = event.user
        val effectiveDate = getEffectiveFortuneDate()

        try {
            if (user.connectCode == null) {
                user.connectCode = connectionService.generateUniqueCode()
                userRepository.save(user)
                log.info("신규 유저 초대 코드 발급: userId={}", user.id)
            }
        } catch (e: Exception) {
            log.warn("신규 유저 초대 코드 발급 실패: userId={}, error={}", user.id, e.message)
        }

        try {
            fortuneService.generateFortune(user, effectiveDate)
            log.info("신규 유저 예감 생성 완료: userId={}, date={}", user.id, effectiveDate)
        } catch (e: Exception) {
            log.warn("신규 유저 예감 생성 실패: userId={}, error={}", user.id, e.message)
        }

        try {
            val currentRound = lottoService.getCurrentRound()
            lottoService.recommend(user, currentRound)
            log.info("신규 유저 로또 추천 생성 완료: userId={}, round={}", user.id, currentRound)
        } catch (e: Exception) {
            log.warn("신규 유저 로또 추천 생성 실패: userId={}, error={}", user.id, e.message)
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun handleConnectionCreated(event: ConnectionCreatedEvent) {
        val effectiveDate = getEffectiveFortuneDate()
        try {
            compatibilityService.generateCompatibility(event.connection, effectiveDate)
            log.info("신규 연결 궁합 생성 완료: connectionId={}, date={}", event.connection.id, effectiveDate)
        } catch (e: Exception) {
            log.warn("신규 연결 궁합 생성 실패: connectionId={}, error={}", event.connection.id, e.message)
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun handleGroupMemberJoined(event: GroupMemberJoinedEvent) {
        val effectiveDate = getEffectiveFortuneDate()
        try {
            groupCompatibilityService.generateNewMemberCompatibilities(event.group, event.newMember, effectiveDate)
            log.info("신규 그룹 멤버 궁합 생성 완료: groupId={}, userId={}, date={}", event.group.id, event.newMember.id, effectiveDate)
        } catch (e: Exception) {
            log.warn("신규 그룹 멤버 궁합 생성 실패: groupId={}, userId={}, error={}", event.group.id, event.newMember.id, e.message)
        }
    }

    private fun getEffectiveFortuneDate(): LocalDate {
        val now = LocalDateTime.now(KST)
        return if (now.hour >= NEXT_DAY_CUTOFF_HOUR) now.toLocalDate().plusDays(1) else now.toLocalDate()
    }
}
