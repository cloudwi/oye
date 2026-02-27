package com.mindbridge.oye.service

import com.mindbridge.oye.repository.UserConnectionRepository
import com.mindbridge.oye.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
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

    companion object {
        private const val BATCH_SIZE = 50
        private const val BATCH_DELAY_MS = 500L
    }

    @Scheduled(cron = "0 0 6 * * *", zone = "Asia/Seoul")
    fun generateDailyFortunes() {
        log.info("일일 예감 스케줄러 시작")

        var page = 0
        var successCount = 0
        var failCount = 0
        var totalCount = 0

        do {
            val userPage = userRepository.findAll(PageRequest.of(page, BATCH_SIZE))
            totalCount = userPage.totalElements.toInt()

            for (user in userPage.content) {
                try {
                    fortuneService.generateFortune(user)
                    successCount++
                } catch (e: Exception) {
                    failCount++
                    log.warn("예감 생성 실패: userId={}, error={}", user.id, e.message)
                }
            }

            page++

            if (userPage.hasNext()) {
                Thread.sleep(BATCH_DELAY_MS)
            }
        } while (userPage.hasNext())

        val failRate = if (totalCount > 0) "%.1f".format(failCount * 100.0 / totalCount) else "0.0"
        log.info("일일 예감 스케줄러 완료: 성공={}, 실패={}, 전체={}, 실패율={}%", successCount, failCount, totalCount, failRate)
    }

    @Scheduled(cron = "0 10 6 * * *", zone = "Asia/Seoul")
    fun generateDailyCompatibilities() {
        log.info("일일 궁합 스케줄러 시작")

        val connections = userConnectionRepository.findAllWithUsers()
        var successCount = 0
        var failCount = 0

        connections.chunked(BATCH_SIZE).forEachIndexed { batchIndex, batch ->
            for (connection in batch) {
                try {
                    compatibilityService.generateCompatibility(connection)
                    successCount++
                } catch (e: Exception) {
                    failCount++
                    log.warn("궁합 생성 실패: connectionId={}, error={}", connection.id, e.message)
                }
            }

            if (batchIndex < (connections.size - 1) / BATCH_SIZE) {
                Thread.sleep(BATCH_DELAY_MS)
            }
        }

        val totalCount = connections.size
        val failRate = if (totalCount > 0) "%.1f".format(failCount * 100.0 / totalCount) else "0.0"
        log.info("일일 궁합 스케줄러 완료: 성공={}, 실패={}, 전체={}, 실패율={}%", successCount, failCount, totalCount, failRate)
    }
}
