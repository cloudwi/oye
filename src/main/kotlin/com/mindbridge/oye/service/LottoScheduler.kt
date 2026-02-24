package com.mindbridge.oye.service

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

@Component
class LottoScheduler(
    private val lottoDrawService: LottoDrawService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private val LOTTO_EPOCH: LocalDate = LocalDate.of(2002, 12, 7)
    }

    @Scheduled(cron = "0 0 6 * * SUN")
    fun fetchAndEvaluateLatestDraw() {
        val round = getLastSaturdayRound()
        log.info("로또 스케줄러 시작: 회차 {}", round)

        try {
            val lottoRound = lottoDrawService.fetchDrawResult(round)
            lottoDrawService.evaluateRecommendations(lottoRound)
            log.info("로또 스케줄러 완료: 회차 {}", round)
        } catch (e: Exception) {
            log.error("로또 스케줄러 실패: 회차 {}, error={}", round, e.message, e)
        }
    }

    private fun getLastSaturdayRound(): Int {
        val lastSaturday = LocalDate.now().with(TemporalAdjusters.previous(DayOfWeek.SATURDAY))
        val daysSinceEpoch = ChronoUnit.DAYS.between(LOTTO_EPOCH, lastSaturday)
        return (daysSinceEpoch / 7 + 1).toInt()
    }
}
