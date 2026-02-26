package com.mindbridge.oye.service

import com.mindbridge.oye.repository.UserConnectionRepository
import com.mindbridge.oye.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class DailyFortuneScheduler(
    private val userRepository: UserRepository,
    private val userConnectionRepository: UserConnectionRepository,
    private val fortuneService: FortuneService,
    private val compatibilityService: CompatibilityService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(cron = "0 0 6 * * *", zone = "Asia/Seoul")
    fun generateDailyFortunes() {
        log.info("일일 예감 스케줄러 시작")

        val users = userRepository.findAll()
        var successCount = 0
        var failCount = 0

        for (user in users) {
            try {
                fortuneService.generateFortune(user)
                successCount++
            } catch (e: Exception) {
                failCount++
                log.warn("예감 생성 실패: userId={}, error={}", user.id, e.message)
            }
        }

        log.info("일일 예감 스케줄러 완료: 성공={}, 실패={}, 전체={}", successCount, failCount, users.size)
    }

    @Scheduled(cron = "0 10 6 * * *", zone = "Asia/Seoul")
    fun generateDailyCompatibilities() {
        log.info("일일 궁합 스케줄러 시작")

        val connections = userConnectionRepository.findAllWithUsers()
        var successCount = 0
        var failCount = 0

        for (connection in connections) {
            try {
                compatibilityService.generateCompatibility(connection)
                successCount++
            } catch (e: Exception) {
                failCount++
                log.warn("궁합 생성 실패: connectionId={}, error={}", connection.id, e.message)
            }
        }

        log.info("일일 궁합 스케줄러 완료: 성공={}, 실패={}, 전체={}", successCount, failCount, connections.size)
    }
}
