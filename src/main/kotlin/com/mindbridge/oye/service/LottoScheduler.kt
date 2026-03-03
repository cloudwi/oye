package com.mindbridge.oye.service

import com.mindbridge.oye.exception.LottoAlreadyRecommendedException
import com.mindbridge.oye.repository.LottoRecommendationRepository
import com.mindbridge.oye.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

@Component
class LottoScheduler(
    private val lottoDrawService: LottoDrawService,
    private val lottoService: LottoService,
    private val userRepository: UserRepository,
    private val lottoRecommendationRepository: LottoRecommendationRepository,
    private val pushNotificationService: PushNotificationService
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

        pushNotificationService.sendToAll("이번 주 로또 추천 번호가 도착했어요!", "AI가 분석한 행운의 번호를 확인해보세요.")
    }

    @Scheduled(cron = "0 0 22 * * SAT")
    fun fetchAndEvaluateLatestDraw() {
        val round = getLastSaturdayRound()
        log.info("로또 결과 스케줄러 시작: 회차 {}", round)

        try {
            val lottoRound = lottoDrawService.fetchDrawResult(round)
            lottoDrawService.evaluateRecommendations(lottoRound)
            log.info("로또 결과 스케줄러 완료: 회차 {}", round)

            sendLottoResultPush(round)
        } catch (e: Exception) {
            log.error("로또 결과 스케줄러 실패: 회차 {}, error={}", round, e.message, e)
        }
    }

    private fun sendLottoResultPush(round: Int) {
        try {
            val recommendations = lottoRecommendationRepository.findByRound(round)
            val userRecommendations = recommendations.groupBy { it.user.id!! }

            for ((_, recs) in userRecommendations) {
                val user = recs.first().user
                val hasWin = recs.any { it.rank != null }
                if (hasWin) {
                    val bestRank = recs.filter { it.rank != null }.minByOrNull { it.rank!!.ordinal }!!
                    pushNotificationService.sendToUser(
                        user,
                        "${round}회차 ${bestRank.rank!!.name} 당첨! 축하합니다!",
                        "당첨 결과를 확인해보세요."
                    )
                } else {
                    pushNotificationService.sendToUser(
                        user,
                        "${round}회차 추첨 결과",
                        "이번 회차는 아쉽게 당첨되지 않았어요."
                    )
                }
            }
        } catch (e: Exception) {
            log.warn("로또 결과 푸시 전송 실패: round={}, error={}", round, e.message)
        }
    }

    private fun getLastSaturdayRound(): Int {
        val lastSaturday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.SATURDAY))
        return lottoService.getRoundForDate(lastSaturday)
    }
}
