package com.mindbridge.oye.service

import com.mindbridge.oye.exception.LottoAlreadyRecommendedException
import com.mindbridge.oye.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

@Component
class LottoScheduler(
    private val lottoDrawService: LottoDrawService,
    private val lottoService: LottoService,
    private val userRepository: UserRepository
) {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val BATCH_SIZE = 50
        private const val BATCH_DELAY_MS = 500L
    }

    @Scheduled(cron = "0 0 6 * * SUN")
    fun generateWeeklyRecommendations() {
        val round = lottoService.getCurrentRound()
        log.info("로또 추천 스케줄러 시작: 회차 {}", round)

        var page = 0
        var successCount = 0
        var skipCount = 0
        var failCount = 0
        var totalCount = 0

        do {
            val userPage = userRepository.findAll(PageRequest.of(page, BATCH_SIZE))
            totalCount = userPage.totalElements.toInt()

            for (user in userPage.content) {
                try {
                    lottoService.recommend(user, round)
                    successCount++
                } catch (e: LottoAlreadyRecommendedException) {
                    skipCount++
                } catch (e: Exception) {
                    failCount++
                    log.warn("로또 추천 생성 실패: userId={}, round={}, error={}", user.id, round, e.message)
                }
            }

            page++

            if (userPage.hasNext()) {
                Thread.sleep(BATCH_DELAY_MS)
            }
        } while (userPage.hasNext())

        log.info("로또 추천 스케줄러 완료: 회차={}, 성공={}, 스킵={}, 실패={}, 전체={}", round, successCount, skipCount, failCount, totalCount)
    }

    @Scheduled(cron = "0 0 22 * * SAT")
    fun fetchAndEvaluateLatestDraw() {
        val round = getLastSaturdayRound()
        log.info("로또 결과 스케줄러 시작: 회차 {}", round)

        try {
            val lottoRound = lottoDrawService.fetchDrawResult(round)
            lottoDrawService.evaluateRecommendations(lottoRound)
            log.info("로또 결과 스케줄러 완료: 회차 {}", round)
        } catch (e: Exception) {
            log.error("로또 결과 스케줄러 실패: 회차 {}, error={}", round, e.message, e)
        }
    }

    private fun getLastSaturdayRound(): Int {
        val lastSaturday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.SATURDAY))
        return lottoService.getRoundForDate(lastSaturday)
    }
}
