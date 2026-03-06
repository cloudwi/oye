package com.mindbridge.oye.service

import com.mindbridge.oye.repository.GroupMemberRepository
import com.mindbridge.oye.repository.GroupRepository
import com.mindbridge.oye.repository.UserConnectionRepository
import com.mindbridge.oye.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.PageRequest
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class DailyFortuneScheduler(
    private val userRepository: UserRepository,
    private val userConnectionRepository: UserConnectionRepository,
    private val groupRepository: GroupRepository,
    private val groupMemberRepository: GroupMemberRepository,
    private val fortuneService: FortuneService,
    private val compatibilityService: CompatibilityService,
    private val groupCompatibilityService: GroupCompatibilityService,
    private val pushNotificationService: PushNotificationService
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
                    pushNotificationService.sendToUser(user, "오늘의 예감이 도착했어요!", "지금 확인해보세요.")
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

        if (totalCount > 0) {
            val failRate = "%.1f".format(failCount * 100.0 / totalCount)
            log.info("일일 예감 스케줄러 완료: 성공={}, 실패={}, 전체={}, 실패율={}%", successCount, failCount, totalCount, failRate)
        }
    }

    @Scheduled(cron = "0 0 6 * * *", zone = "Asia/Seoul")
    fun generateDailyCompatibilities() {
        log.info("일일 궁합 스케줄러 시작")

        var page = 0
        var successCount = 0
        var failCount = 0
        var skipCount = 0
        var totalCount = 0

        do {
            val connectionPage = userConnectionRepository.findAllWithUsers(PageRequest.of(page, BATCH_SIZE))
            totalCount = connectionPage.totalElements.toInt()

            for (connection in connectionPage.content) {
                try {
                    compatibilityService.generateCompatibility(connection)
                    successCount++
                    pushNotificationService.sendToUsers(
                        listOf(connection.user, connection.partner),
                        "오늘의 궁합이 도착했어요!",
                        "지금 확인해보세요."
                    )
                } catch (e: DataIntegrityViolationException) {
                    skipCount++
                } catch (e: Exception) {
                    failCount++
                    log.warn("궁합 생성 실패: connectionId={}, error={}", connection.id, e.message)
                }
            }

            page++

            if (connectionPage.hasNext()) {
                Thread.sleep(BATCH_DELAY_MS)
            }
        } while (connectionPage.hasNext())

        if (totalCount > 0) {
            val failRate = "%.1f".format(failCount * 100.0 / totalCount)
            log.info("일일 궁합 스케줄러 완료: 성공={}, 스킵={}, 실패={}, 전체={}, 실패율={}%", successCount, skipCount, failCount, totalCount, failRate)
        }
    }

    @Scheduled(cron = "0 0 6 * * *", zone = "Asia/Seoul")
    fun generateDailyGroupCompatibilities() {
        log.info("일일 그룹 궁합 스케줄러 시작")

        var page = 0
        var successCount = 0
        var failCount = 0
        var totalCount = 0

        do {
            val groupPage = groupRepository.findAllWithOwner(PageRequest.of(page, BATCH_SIZE))
            totalCount = groupPage.totalElements.toInt()

            for (group in groupPage.content) {
                try {
                    groupCompatibilityService.generateGroupCompatibility(group)
                    successCount++
                    val members = groupMemberRepository.findByGroupWithUsers(group)
                    pushNotificationService.sendToUsers(
                        members.map { it.user },
                        "${group.name} 그룹 궁합이 도착했어요!",
                        "지금 확인해보세요."
                    )
                } catch (e: Exception) {
                    failCount++
                    log.warn("그룹 궁합 생성 실패: groupId={}, error={}", group.id, e.message)
                }
            }

            page++

            if (groupPage.hasNext()) {
                Thread.sleep(BATCH_DELAY_MS)
            }
        } while (groupPage.hasNext())

        if (totalCount > 0) {
            val failRate = "%.1f".format(failCount * 100.0 / totalCount)
            log.info("일일 그룹 궁합 스케줄러 완료: 성공={}, 실패={}, 전체={}, 실패율={}%", successCount, failCount, totalCount, failRate)
        }
    }
}
