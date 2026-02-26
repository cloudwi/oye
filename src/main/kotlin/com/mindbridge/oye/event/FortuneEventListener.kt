package com.mindbridge.oye.event

import com.mindbridge.oye.service.CompatibilityService
import com.mindbridge.oye.service.FortuneService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class FortuneEventListener(
    private val fortuneService: FortuneService,
    private val compatibilityService: CompatibilityService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handleUserCreated(event: UserCreatedEvent) {
        try {
            fortuneService.generateFortune(event.user)
            log.info("신규 유저 예감 생성 완료: userId={}", event.user.id)
        } catch (e: Exception) {
            log.warn("신규 유저 예감 생성 실패: userId={}, error={}", event.user.id, e.message)
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handleConnectionCreated(event: ConnectionCreatedEvent) {
        try {
            compatibilityService.generateCompatibility(event.connection)
            log.info("신규 연결 궁합 생성 완료: connectionId={}", event.connection.id)
        } catch (e: Exception) {
            log.warn("신규 연결 궁합 생성 실패: connectionId={}, error={}", event.connection.id, e.message)
        }
    }
}
